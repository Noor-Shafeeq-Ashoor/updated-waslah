
package com.ga.workspace_service.bdd;

import com.ga.workspace_service.dto.BookingRequest;
import com.ga.workspace_service.dto.BookingResponse;
import com.ga.workspace_service.exception.ConflictException;
import com.ga.workspace_service.model.*;
import com.ga.workspace_service.repository.*;
import com.ga.workspace_service.security.CurrentUser;
import com.ga.workspace_service.security.JwtPrincipal;
import com.ga.workspace_service.service.*;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class BookingSteps {

    private BookingService bookingService;
    private BookingRepository bookingRepository;
    private StudySpaceRepository studySpaceRepository;
    private CurrentUser currentUser;
    private IdentityClient identityClient;
    private AuditService auditService;
    private ApplicationEventPublisher eventPublisher;
    private ParticipationLockService participationLockService;

    private StudySpace studySpace;
    private BookingRequest request;
    private BookingResponse response;
    private Exception failure;

    @Before
    public void setUp() {
        bookingRepository = mock(BookingRepository.class);
        studySpaceRepository = mock(StudySpaceRepository.class);
        currentUser = mock(CurrentUser.class);
        identityClient = mock(IdentityClient.class);
        auditService = mock(AuditService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        participationLockService = mock(ParticipationLockService.class);

        bookingService = new BookingService(
                bookingRepository,
                studySpaceRepository,
                currentUser,
                identityClient,
                auditService,
                eventPublisher,
                mock(StudySessionRepository.class),
                participationLockService,
                mock(SessionParticipantRepository.class),
                mock(SessionWaitlistRepository.class),
                mock(SessionActivityService.class)
        );

        Cafe cafe = new Cafe();
        cafe.setId(1L);
        cafe.setName("WASLAH Test Cafe");
        cafe.setManagerUserId(99L);

        studySpace = new StudySpace();
        studySpace.setId(10L);
        studySpace.setName("Quiet Study Room");
        studySpace.setCapacity(4);
        studySpace.setAvailable(true);
        studySpace.setCafe(cafe);

        request = new BookingRequest();
        request.setStudySpaceId(10L);
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));

        when(currentUser.get()).thenReturn(
                new JwtPrincipal(1L, "testuser", Role.USER)
        );

        when(studySpaceRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(studySpace));

        when(identityClient.username(1L)).thenReturn("testuser");

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> {
                    Booking booking = invocation.getArgument(0);
                    booking.setId(100L);
                    return booking;
                });
    }

    @Given("an available study space")
    public void availableStudySpace() {
        assertTrue(studySpace.getAvailable());
    }

    @Given("an existing booking for a study space")
    public void existingBooking() {
        when(bookingRepository
                .existsByStudySpaceAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                        eq(studySpace),
                        anyList(),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)
                ))
                .thenReturn(true);
    }

    @Given("a study space that is unavailable")
    public void unavailableStudySpace() {
        studySpace.setAvailable(false);
    }

    @When("a user books the study space")
    public void userBooksStudySpace() {
        executeBooking();
    }

    @When("another user requests an overlapping booking")
    public void overlappingBooking() {
        executeBooking();
    }

    private void executeBooking() {
        try {
            response = bookingService.createBooking("testuser", request);
        } catch (Exception exception) {
            failure = exception;
        }
    }

    @Then("the booking should be created successfully")
    public void bookingCreated() {
        assertNull(failure);
        assertNotNull(response);
        assertNotNull(response.getId());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Then("the booking status should be pending")
    public void bookingPending() {
        assertEquals(BookingStatus.PENDING, response.getStatus());
    }

    @Then("the second booking should be rejected")
    public void overlappingBookingRejected() {
        assertInstanceOf(ConflictException.class, failure);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Then("the booking should be rejected")
    public void unavailableBookingRejected() {
        assertInstanceOf(ConflictException.class, failure);
        verify(bookingRepository, never()).save(any(Booking.class));
    }
}
