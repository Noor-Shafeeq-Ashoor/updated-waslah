
package com.ga.workspace_service.service;

import com.ga.workspace_service.model.*;
import com.ga.workspace_service.repository.BookingRepository;
import com.ga.workspace_service.repository.SessionWaitlistRepository;
import com.ga.workspace_service.repository.StudySessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Automatically completes expired bookings and their linked
 * study sessions.
 *
 * Uses the shared PostgreSQL advisory lock to prevent races
 * with booking cancellation and study-session operations.
 */
@Service
@ConditionalOnProperty(
        name = "waslah.booking.scheduler.enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Slf4j
@RequiredArgsConstructor
public class BookingCompletionScheduler {

    private final BookingRepository bookingRepository;
    private final StudySessionRepository studySessionRepository;
    private final ParticipationLockService participationLockService;
    private final SessionWaitlistRepository sessionWaitlistRepository;
    private final SessionActivityService sessionActivityService;
    private final AuditService auditService;

    /**
     * Runs every minute to cancel expired pending bookings
     * and complete confirmed bookings whose end time has passed.
     *
     * Booking and session updates happen atomically
     * inside the same database transaction.
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void completeExpiredBookings() {

        participationLockService.lockStudySessionWrites();

        LocalDateTime now = LocalDateTime.now();

        // Cancel expired pending bookings.
        List<Booking> expiredPending =
                bookingRepository.findByStatusAndEndTimeLessThan(
                        BookingStatus.PENDING,
                        now
                );

        for (Booking booking : expiredPending) {
            booking.setStatus(BookingStatus.CANCELLED);
            auditService.log(
                    "SYSTEM",
                    "BOOKING_EXPIRED",
                    "BOOKING",
                    booking.getId()
            );
        }

        if (!expiredPending.isEmpty()) {
            bookingRepository.saveAll(expiredPending);

            log.info(
                    "Automatically cancelled {} expired pending bookings",
                    expiredPending.size()
            );
        }

        // Complete expired confirmed bookings.
        List<Booking> expiredBookings =
                bookingRepository.findByStatusAndEndTimeLessThan(
                        BookingStatus.CONFIRMED,
                        now
                );

        if (expiredBookings.isEmpty()) {
            return;
        }

        int completedSessions = 0;

        for (Booking booking : expiredBookings) {

            booking.setStatus(BookingStatus.COMPLETED);

            // A booking may or may not have a linked study session.
            StudySession session = studySessionRepository
                    .findByBooking_Id(booking.getId())
                    .orElse(null);

            if (session != null &&
                    (session.getStatus() == StudySessionStatus.OPEN ||
                            session.getStatus() == StudySessionStatus.FULL)) {

                session.setStatus(StudySessionStatus.COMPLETED);

                // Waiting participants can no longer be promoted.
                sessionWaitlistRepository.deleteBySession_Id(
                        session.getId()
                );

                sessionActivityService.record(
                        session,
                        null,
                        SessionEventType.COMPLETED,
                        "Booking " + booking.getId() + " ended"
                );

                completedSessions++;
            }
        }

        // Persist completed bookings in the same transaction.
        bookingRepository.saveAll(expiredBookings);

        log.info(
                "Automatically completed {} bookings and {} study sessions",
                expiredBookings.size(),
                completedSessions
        );
    }
}
