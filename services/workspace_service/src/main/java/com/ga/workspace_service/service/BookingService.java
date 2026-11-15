package com.ga.workspace_service.service;

import com.ga.workspace_service.dto.*;

import com.ga.workspace_service.event.BookingCreatedEvent;

import com.ga.workspace_service.exception.*;

import com.ga.workspace_service.model.*;

import com.ga.workspace_service.repository.*;

import com.ga.workspace_service.security.CurrentUser;
import com.ga.workspace_service.security.JwtPrincipal;
import com.ga.workspace_service.util.*;


import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final StudySpaceRepository studySpaceRepository;
    private final CurrentUser currentUser;
    private final IdentityClient identityClient;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final StudySessionRepository studySessionRepository;
    private final ParticipationLockService participationLockService;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final SessionWaitlistRepository sessionWaitlistRepository;
    private final SessionActivityService sessionActivityService;

    public BookingService(
            BookingRepository bookingRepository,
            StudySpaceRepository studySpaceRepository,
            CurrentUser currentUser,
            IdentityClient identityClient,
            AuditService auditService,
            ApplicationEventPublisher eventPublisher,
            StudySessionRepository studySessionRepository,
            ParticipationLockService participationLockService,
            SessionParticipantRepository sessionParticipantRepository,
            SessionWaitlistRepository sessionWaitlistRepository,
            SessionActivityService sessionActivityService

    ) {
        this.bookingRepository = bookingRepository;
        this.studySpaceRepository = studySpaceRepository;
        this.currentUser = currentUser;
        this.identityClient = identityClient;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
        this.studySessionRepository = studySessionRepository;
        this.participationLockService = participationLockService;
        this.sessionParticipantRepository = sessionParticipantRepository;
        this.sessionWaitlistRepository = sessionWaitlistRepository;
        this.sessionActivityService = sessionActivityService;
    }


    /**Retrieves bookings using optional filters, pagination, and sorting.*/
    public PageResponse<BookingResponse> getBookings(
            BookingStatus status,
            Long studySpaceId,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size,
            String sort,
            String direction
    ) {
        // Reject a date range that starts after it ends.
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "'from' must be earlier than or equal to 'to'."
            );
        }

        // Build the query using only filters the client supplied.
        Specification<Booking> specification =
                Specs.<Booking>equal(status, "status")
                        .and(Specs.<Booking>equal(studySpaceId, "studySpace.id"))
                        .and(Specs.atLeast(from, "startTime"))
                        .and(Specs.atMost(to, "startTime"));

        // Accept only the supported sort fields.
        Pageable pageable = PagingUtil.createPageable(page, size, sort, direction, "startTime",
                Set.of("startTime", "endTime", "createdAt", "status")
        );

        // Execute filtering and pagination in the database.
        Page<Booking> bookingPage = bookingRepository.findAll(
                specification,
                pageable
        );

        // Convert each booking entity to its response DTO.
        List<BookingResponse> content = bookingPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        // Return the results and pagination metadata.
        return new PageResponse<>(
                content,
                bookingPage.getNumber(),
                bookingPage.getSize(),
                bookingPage.getTotalElements(),
                bookingPage.getTotalPages(),
                bookingPage.isFirst(),
                bookingPage.isLast()
        );
    }


    /**
     * Retrieves only the authenticated user's bookings with optional filters.
     */
    public PageResponse<BookingResponse> getMyBookings(
            String username,
            BookingStatus status,
            Long studySpaceId,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size,
            String sort,
            String direction
    ) {
        // Validate the date range before querying the database.
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "'from' must be earlier than or equal to 'to'."
            );
        }

        // Restrict every result to the authenticated user's bookings.
        Specification<Booking> specification =
                Specs.<Booking>equal(currentUser.get().id(), "userId")
                        .and(Specs.equal(status, "status"))
                        .and(Specs.equal(studySpaceId, "studySpace.id"))
                        .and(Specs.atLeast(from, "startTime"))
                        .and(Specs.atMost(to, "startTime"));

        Pageable pageable = PagingUtil.createPageable(
                page,
                size,
                sort,
                direction,
                "startTime",
                Set.of("startTime", "endTime", "createdAt", "status")
        );

        Page<Booking> bookingPage = bookingRepository.findAll(
                specification,
                pageable
        );

        List<BookingResponse> content = bookingPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(
                content,
                bookingPage.getNumber(),
                bookingPage.getSize(),
                bookingPage.getTotalElements(),
                bookingPage.getTotalPages(),
                bookingPage.isFirst(),
                bookingPage.isLast()
        );
    }





    // =========================
    // CREATE BOOKING
    // =========================

    @Transactional
    public BookingResponse createBooking(
            String username,
            BookingRequest request
    ) {
        // Acquire the shared lock before taking any booking or study-space row locks.
        participationLockService.lockStudySessionWrites();

        JwtPrincipal user = currentUser.get();

        StudySpace studySpace = studySpaceRepository
                .findByIdForUpdate(request.getStudySpaceId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Study space not found"));

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            log.warn("Booking rejected: invalid time range");

            throw new IllegalArgumentException(
                    "End time must be after start time"
            );
        }

        if (!studySpace.getAvailable()) {
            log.warn("Booking rejected: study space is unavailable");

            throw new ConflictException(
                    "This study space is not available"
            );
        }

        boolean alreadyBooked =
                bookingRepository
                        .existsByStudySpaceAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                                studySpace,
                                List.of(
                                        BookingStatus.PENDING,
                                        BookingStatus.CONFIRMED
                                ),
                                request.getEndTime(),
                                request.getStartTime()
                        );


        if (alreadyBooked) {
            // A valid booking request conflicts with an existing reservation.
            // HTTP 409 is more appropriate than HTTP 400.
            log.warn("Booking rejected: time slot conflict");

            throw new ConflictException(
                    "This study space is already booked for the selected time"
            );
        }


        Booking booking = new Booking();
        booking.setUserId(user.id());
        booking.setStudySpace(studySpace);
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setStatus(BookingStatus.PENDING);

        Booking savedBooking = bookingRepository.save(booking);
        eventPublisher.publishEvent(
                new BookingCreatedEvent(
                        savedBooking.getId(),
                        savedBooking.getUserId(),
                        savedBooking.getStudySpace()
                                .getCafe()
                                .getManagerUserId()
                )
        );

        auditService.log(
                username,
                "BOOKING_CREATED",
                "BOOKING",
                savedBooking.getId()
        );

        log.info("Booking created successfully with id={}", savedBooking.getId());

        return toResponse(savedBooking);
    }

    // =========================
    // GET MY BOOKINGS
    // =========================

    public List<BookingResponse> getMyBookings(String username) {

        List<BookingResponse> bookings = bookingRepository
                .findByUserId(currentUser.get().id())
                .stream()
                .map(this::toResponse)
                .toList();

        log.debug("Retrieved bookings for user");

        return bookings;
    }

    // =========================
    // GET BOOKING BY ID
    // =========================

    public BookingResponse getBookingById(
            Long id,
            String username
    ) {

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Booking not found"
                        ));

        checkOwnership(booking, username);

        log.debug("Booking retrieved successfully with id={}", id);

        return toResponse(booking);
    }

    // =========================
    // CANCEL BOOKING
    // =========================

    /**
     * Cancels a booking and its associated study session atomically.
     * All related writers must acquire the shared advisory lock first.
     * The booking row is locked second, so concurrent confirmation and
     * cancellation cannot make incompatible decisions.
     *
     * @param id booking identifier
     * @param username authenticated booking owner
     * @return cancelled booking details
     */
    @Transactional
    public BookingResponse cancelBooking(Long id, String username) {

        // 1. Coordinate with session creation, joining and promotion.
        participationLockService.lockStudySessionWrites();

        // 2. Serialize changes to this booking's status.
        Booking booking = bookingRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Booking not found")
                );

        // 3. Only the owner can cancel this booking.
        checkOwnership(booking, username);

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new ConflictException("Completed bookings cannot be cancelled");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ConflictException("Booking is already cancelled");
        }

        // 4. Keep session/participant history, but make the session inactive
        //    and tell everyone who was in or waiting for it.
        cancelLinkedSession(booking, username);

        // 5. Both status changes commit or roll back together.
        booking.setStatus(BookingStatus.CANCELLED);
        Booking savedBooking = bookingRepository.save(booking);

        auditService.log(
                username,
                "BOOKING_CANCELLED",
                "BOOKING",
                savedBooking.getId()
        );

        log.info("Booking {} and its associated session cancelled", id);
        return toResponse(savedBooking);
    }

    // =========================
    // HELPER METHODS
    // =========================

    private void checkOwnership(
            Booking booking,
            String username
    ) {

        if (!booking.getUserId().equals(currentUser.get().id())) {

            log.warn("Booking access denied: ownership check failed");

            throw new ForbiddenException(
                    "You are not allowed to access this booking"
            );
        }
    }

    private BookingResponse toResponse(Booking booking) {

        return new BookingResponse(
                booking.getId(),
                identityClient.username(booking.getUserId()),
                booking.getStudySpace().getCafe().getName(),
                booking.getStudySpace().getName(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getStatus(),
                booking.getCreatedAt(),
                booking.getStudySpace().getId(),
                booking.getStudySpace().getCapacity()
        );
    }

    /**
     * Confirms a pending booking.
     * Only the assigned cafe manager can confirm it.
     */
    @Transactional
    public BookingResponse confirmBooking(
            Long bookingId,
            String managerUsername
    ) {

        // Coordinate with all study-session and booking status writes first.
        participationLockService.lockStudySessionWrites();

        // Retrieve and lock the booking.
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Booking not found")
                );

        // Check that the authenticated user manages this cafe.
        Long assignedManager = booking.getStudySpace()
                .getCafe()
                .getManagerUserId();

        if (!assignedManager.equals(currentUser.get().id())) {
            throw new ForbiddenException(
                    "You are not allowed to confirm this booking"
            );
        }

        // Only PENDING bookings can become CONFIRMED.
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ConflictException(
                    "Only pending bookings can be confirmed"
            );
        }

        // A booking whose time has already passed can no longer be confirmed.
        if (!booking.getEndTime().isAfter(LocalDateTime.now())) {
            throw new ConflictException(
                    "This booking has already ended and cannot be confirmed"
            );
        }

        // Update booking status.
        booking.setStatus(BookingStatus.CONFIRMED);

        Booking savedBooking = bookingRepository.save(booking);

        // Record the confirmation.
        auditService.log(
                managerUsername,
                "BOOKING_CONFIRMED",
                "BOOKING",
                savedBooking.getId()
        );

        log.info("Booking confirmed: id={}", savedBooking.getId());

        return toResponse(savedBooking);
    }


    /**
     * Lets the assigned cafe manager reject a PENDING booking or cancel a
     * CONFIRMED one. Uses the same lock order as every other writer
     * (advisory lock first, booking row second), so it cannot overwrite a
     * concurrent cancellation or confirmation.
     *
     * <p>Cancelling a confirmed booking also cancels its linked study session.</p>
     */
    @Transactional
    public BookingResponse rejectBooking(Long bookingId, String managerUsername) {

        participationLockService.lockStudySessionWrites();

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Booking not found")
                );

        Long assignedManager = booking.getStudySpace()
                .getCafe()
                .getManagerUserId();

        if (!assignedManager.equals(currentUser.get().id())) {
            throw new ForbiddenException(
                    "You are not allowed to manage this booking"
            );
        }

        BookingStatus previous = booking.getStatus();

        if (previous != BookingStatus.PENDING
                && previous != BookingStatus.CONFIRMED) {
            throw new ConflictException(
                    "Only pending or confirmed bookings can be rejected or cancelled"
            );
        }

        cancelLinkedSession(booking, managerUsername);

        booking.setStatus(BookingStatus.CANCELLED);
        Booking savedBooking = bookingRepository.save(booking);

        auditService.log(
                managerUsername,
                previous == BookingStatus.PENDING
                        ? "BOOKING_REJECTED"
                        : "BOOKING_CANCELLED_BY_MANAGER",
                "BOOKING",
                savedBooking.getId()
        );

        log.info("Booking {} ({}) set to CANCELLED by its cafe manager",
                bookingId, previous);

        return toResponse(savedBooking);
    }

    /**
     * Cancels the study session linked to a booking (if it is still active),
     * removes its waitlist, records the change in the session history and
     * notifies everyone affected. Must be called while holding the
     * participation lock.
     */
    private void cancelLinkedSession(Booking booking, String actor) {
        studySessionRepository.findByBooking_Id(booking.getId()).ifPresent(session -> {
            if (session.getStatus() == StudySessionStatus.COMPLETED) {
                throw new ConflictException(
                        "Cannot cancel a booking with a completed study session"
                );
            }
            if (session.getStatus() == StudySessionStatus.CANCELLED) {
                return;
            }

            List<Long> affected = new ArrayList<>();
            for (SessionParticipant participant
                    : sessionParticipantRepository.findBySession_Id(session.getId())) {
                affected.add(participant.getUserId());
            }
            for (SessionWaitlist waiting
                    : sessionWaitlistRepository.findBySession_IdOrderByPositionAsc(session.getId())) {
                affected.add(waiting.getUserId());
            }
            Long actorId = currentUser.get().id();
            affected.removeIf(actorId::equals);

            // No one can be promoted into a cancelled session.
            sessionWaitlistRepository.deleteBySession_Id(session.getId());
            session.setStatus(StudySessionStatus.CANCELLED);

            sessionActivityService.record(session, null, SessionEventType.CANCELLED,
                    "Booking " + booking.getId() + " was cancelled by " + actor,
                    affected,
                    "The study session \"" + session.getTitle()
                            + "\" was cancelled because its booking was cancelled");
        });
    }
}
