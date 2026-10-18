package com.ga.waslah.service;

import com.ga.waslah.dto.BookingRequest;
import com.ga.waslah.dto.BookingResponse;
import com.ga.waslah.exception.ForbiddenException;
import com.ga.waslah.exception.ResourceNotFoundException;
import com.ga.waslah.model.Booking;
import com.ga.waslah.model.BookingStatus;
import com.ga.waslah.model.StudySpace;
import com.ga.waslah.model.User;
import com.ga.waslah.repository.BookingRepository;
import com.ga.waslah.repository.StudySpaceRepository;
import com.ga.waslah.repository.UserRepository;
import com.ga.waslah.dto.PageResponse;
import com.ga.waslah.util.PagingUtil;
import com.ga.waslah.util.Specs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import com.ga.waslah.event.BookingCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final StudySpaceRepository studySpaceRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    public BookingService(
            BookingRepository bookingRepository,
            StudySpaceRepository studySpaceRepository,
            UserRepository userRepository,
            AuditService auditService,
            ApplicationEventPublisher eventPublisher

    ) {
        this.bookingRepository = bookingRepository;
        this.studySpaceRepository = studySpaceRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
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
                        .and(
                                Specs.<Booking>equal(
                                        studySpaceId,
                                        "studySpace.id"
                                )
                        )
                        .and(
                                Specs.atLeast(from, "startTime")
                        )
                        .and(
                                Specs.atMost(to, "startTime")
                        );

        // Accept only the supported sort fields.
        Pageable pageable = PagingUtil.createPageable(
                page,
                size,
                sort,
                direction,
                "startTime",
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
                Specs.<Booking>equal(username, "user.username")
                        .and(Specs.equal(status, "status"))
                        .and(
                                Specs.equal(
                                        studySpaceId,
                                        "studySpace.id"
                                )
                        )
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
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

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

            throw new ForbiddenException(
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
            log.warn("Booking rejected: time slot conflict");

            throw new IllegalArgumentException(
                    "This study space is already booked for the selected time"
            );
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setStudySpace(studySpace);
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setStatus(BookingStatus.PENDING);

        Booking savedBooking = bookingRepository.save(booking);
        eventPublisher.publishEvent(
                new BookingCreatedEvent(
                        savedBooking.getId(),
                        savedBooking.getUser().getUsername(),
                        savedBooking.getStudySpace()
                                .getCafe()
                                .getManager()
                                .getUsername()
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
                .findByUser_Username(username)
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

    public BookingResponse cancelBooking(
            Long id,
            String username
    ) {

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Booking not found"
                        ));

        // A user can only cancel their own booking.
        checkOwnership(booking, username);

        // A completed booking cannot be cancelled.
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            log.warn("Booking cancellation rejected: booking is completed");

            throw new ForbiddenException(
                    "Completed bookings cannot be cancelled"
            );
        }

        // A cancelled booking cannot be cancelled again.
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            log.warn("Booking cancellation rejected: already cancelled");

            throw new ForbiddenException(
                    "Booking is already cancelled"
            );
        }

        booking.setStatus(BookingStatus.CANCELLED);

        Booking savedBooking = bookingRepository.save(booking);

        auditService.log(
                username,
                "BOOKING_CANCELLED",
                "BOOKING",
                savedBooking.getId()
        );

        log.info("Booking cancelled successfully with id={}", id);

        return toResponse(savedBooking);
    }

    // =========================
    // HELPER METHODS
    // =========================

    private void checkOwnership(
            Booking booking,
            String username
    ) {

        if (!booking.getUser()
                .getUsername()
                .equals(username)) {

            log.warn("Booking access denied: ownership check failed");

            throw new ForbiddenException(
                    "You are not allowed to access this booking"
            );
        }
    }

    private BookingResponse toResponse(Booking booking) {

        return new BookingResponse(
                booking.getId(),
                booking.getUser().getUsername(),
                booking.getStudySpace().getCafe().getName(),
                booking.getStudySpace().getName(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getStatus(),
                booking.getCreatedAt()
        );
    }
}
