
package com.ga.waslah.config;

import com.ga.waslah.dto.BookingRequest;
import com.ga.waslah.dto.BookingResponse;
import com.ga.waslah.dto.CreateStudySessionRequest;
import com.ga.waslah.dto.StudySessionResponse;
import com.ga.waslah.model.StudySpace;
import com.ga.waslah.repository.StudySpaceRepository;
import com.ga.waslah.service.BookingService;
import com.ga.waslah.service.StudySessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Automatically creates test data for WASLAH concurrency integration tests.
 * This loader prepares five scenarios:

 *     Two users joining the last available seat.
 *     One user joining two overlapping study sessions.
 *     Two participants leaving while waitlisted users are promoted.
 *     A booking cancellation racing against a session join.

 * The loader is available only in the development profile and
 * must be explicitly enabled using app.seed.concurrency-tests=true.

 * Run against a disposable development database only.
 * Each execution creates new bookings and sessions.
 */
@Slf4j
@Component
@Profile("dev")
@ConditionalOnProperty(
        name = "app.seed.concurrency-tests",
        havingValue = "true"
)
@RequiredArgsConstructor
@Order(100)
public class ConcurrencyTestDataLoader implements CommandLineRunner {

    private final BookingService bookingService;
    private final StudySessionService studySessionService;
    private final StudySpaceRepository studySpaceRepository;

    /**
     * Runs after the normal DataLoader has seeded users and study spaces.
     */
    @Override
    public void run(String... args) {

        log.info("Preparing WASLAH concurrency test fixtures...");

        // Use future dates so booking validation succeeds.
        LocalDateTime base = LocalDateTime.now()
                .plusDays(7)
                .withHour(10)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        // Find study spaces already created by the normal DataLoader.
        Long groupRoomId = findStudySpaceId("Group Room");
        Long meetingRoomId = findStudySpaceId("Meeting Room");

        // =====================================================
        // TEST 1: CONCURRENT JOIN
        // =====================================================
        // Capacity = 2.
        // The organizer occupies one seat.
        // Two users compete for the remaining seat.
        // Expected: one JOINED and one WAITLISTED.

        Fixture concurrentJoin = createFixture(
                "organizer1",
                groupRoomId,
                base,
                base.plusHours(2),
                "Concurrency - Last Seat",
                2
        );

        // =====================================================
        // TEST 2: OVERLAPPING JOIN
        // =====================================================
        // Two different sessions have overlapping time slots.
        // The same user attempts to join both concurrently.
        // Expected: one JOINED and one HTTP 409 Conflict.

        Fixture overlapA = createFixture(
                "organizer1",
                groupRoomId,
                base.plusDays(1),
                base.plusDays(1).plusHours(2),
                "Concurrency - Overlap A",
                3
        );

        Fixture overlapB = createFixture(
                "organizer2",
                meetingRoomId,
                base.plusDays(1).plusHours(1),
                base.plusDays(1).plusHours(3),
                "Concurrency - Overlap B",
                3
        );

        // =====================================================
        // TEST 3: CONCURRENT LEAVE
        // =====================================================
        // Capacity = 3.
        // Organizer + student1 + student2 = FULL.
        // student3 and student4 join the FIFO waitlist.
        // student1 and student2 will leave concurrently.
        // Expected: student3 and student4 are promoted.

        Fixture concurrentLeave = createFixture(
                "organizer1",
                groupRoomId,
                base.plusDays(2),
                base.plusDays(2).plusHours(2),
                "Concurrency - Simultaneous Leave",
                3
        );

        // Fill the remaining two seats.
        studySessionService.joinSession(
                concurrentLeave.sessionId(),
                "student1"
        );

        studySessionService.joinSession(
                concurrentLeave.sessionId(),
                "student2"
        );

        // Add two students to the FIFO waitlist.
        studySessionService.joinSession(
                concurrentLeave.sessionId(),
                "student3"
        );

        studySessionService.joinSession(
                concurrentLeave.sessionId(),
                "student4"
        );

        // =====================================================
        // TEST 4: BOOKING CANCELLATION VS JOIN
        // =====================================================
        // The booking owner cancels the booking while another
        // user attempts to join its associated study session.
        //
        // Expected:
        // Booking cancellation succeeds.
        // The session becomes CANCELLED.
        // No participant may join after cancellation commits.

        Fixture cancellation = createFixture(
                "organizer2",
                meetingRoomId,
                base.plusDays(3),
                base.plusDays(3).plusHours(2),
                "Concurrency - Booking Cancellation",
                3
        );

        // =====================================================
        // PRINT GENERATED TEST IDENTIFIERS
        // =====================================================
        // Copy these IDs into the JUnit Run Configuration.
        // Never assume generated database IDs are fixed.

        log.info("==================================================");
        log.info("WASLAH CONCURRENCY TEST FIXTURES READY");
        log.info("==================================================");

        log.info("WASLAH_TEST_SESSION_ID={}",
                concurrentJoin.sessionId());

        log.info("WASLAH_TEST_OVERLAP_SESSION_A={}",
                overlapA.sessionId());

        log.info("WASLAH_TEST_OVERLAP_SESSION_B={}",
                overlapB.sessionId());

        log.info("WASLAH_TEST_LEAVE_SESSION_ID={}",
                concurrentLeave.sessionId());

        log.info("WASLAH_TEST_CANCEL_BOOKING_ID={}",
                cancellation.bookingId());

        log.info("WASLAH_TEST_CANCEL_SESSION_ID={}",
                cancellation.sessionId());

        log.info("==================================================");
        log.info("Concurrency test fixtures successfully created.");
        log.info("==================================================");
    }

    /**
     * Creates a confirmed booking and its associated study session.
     * The real BookingService and StudySessionService are used
     * to preserve application validation and business rules.
     */
    private Fixture createFixture(
            String organizer,
            Long studySpaceId,
            LocalDateTime start,
            LocalDateTime end,
            String title,
            int capacity
    ) {

        // Step 1: Prepare the booking request.
        BookingRequest bookingRequest = new BookingRequest();

        bookingRequest.setStudySpaceId(studySpaceId);
        bookingRequest.setStartTime(start);
        bookingRequest.setEndTime(end);

        // Step 2: Create a PENDING booking.
        BookingResponse booking = bookingService.createBooking(
                organizer,
                bookingRequest
        );

        // Step 3: Confirm the booking using its cafe manager.
        bookingService.confirmBooking(
                booking.getId(),
                "cafe.manager"
        );

        // Step 4: Prepare the study session request.
        CreateStudySessionRequest sessionRequest =
                new CreateStudySessionRequest(
                        booking.getId(),
                        title,
                        "Automated concurrency integration test fixture",
                        capacity
                );

        // Step 5: Create the study session.
        // The service automatically adds the organizer as a participant.
        StudySessionResponse session =
                studySessionService.createSession(
                        organizer,
                        sessionRequest
                );

        log.info(
                "Created fixture: title={}, bookingId={}, sessionId={}",
                title,
                booking.getId(),
                session.id()
        );

        // Step 6: Return both generated IDs.
        return new Fixture(
                booking.getId(),
                session.id()
        );
    }

    /**
     * Finds a seeded study space by its exact name.
     *
     * @param name name of the study space
     * @return database identifier
     * @throws IllegalStateException if the study space is missing
     *                               or multiple spaces share its name
     */
    private Long findStudySpaceId(String name) {

        List<StudySpace> matches = studySpaceRepository.findAll()
                .stream()
                .filter(space -> name.equals(space.getName()))
                .toList();

        if (matches.size() != 1) {
            throw new IllegalStateException(
                    "Expected exactly one study space named: " + name
            );
        }

        return matches.get(0).getId();
    }

    /**
     * Holds the generated booking and session identifiers.
     */
    private record Fixture(
            Long bookingId,
            Long sessionId
    ) {
    }
}
