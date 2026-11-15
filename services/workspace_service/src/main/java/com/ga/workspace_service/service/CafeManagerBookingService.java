package com.ga.workspace_service.service;

import com.ga.workspace_service.dto.BookingResponse;
import com.ga.workspace_service.model.Booking;
import com.ga.workspace_service.repository.BookingRepository;
import com.ga.workspace_service.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CafeManagerBookingService {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final NotificationPublisher notificationPublisher;
    private final CurrentUser currentUser;
    private final IdentityClient identityClient;

    // =========================
    // GET MY CAFE BOOKINGS
    // =========================

    public List<BookingResponse> getMyCafeBookings(String username) {

        return bookingRepository
                .findByStudySpace_Cafe_ManagerUserId(currentUser.get().id())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================
    // CONFIRM BOOKING
    // =========================

    /**
     * Confirms a pending booking. The state change itself is done by
     * {@link BookingService#confirmBooking}, which takes the shared advisory
     * lock and a row lock, so it cannot race with a cancellation. This class
     * only adds the notification.
     */
    public BookingResponse confirmBooking(
            Long bookingId,
            String username
    ) {

        BookingResponse response = bookingService.confirmBooking(
                bookingId,
                username
        );

        notifyOwner(response, "BOOKING_CONFIRMED",
                "Your booking has been confirmed. Booking ID: " + response.getId());

        return response;
    }

    // =========================
    // REJECT / CANCEL BOOKING
    // =========================

    /**
     * Rejects a pending booking or cancels a confirmed one (together with its
     * study session). See {@link BookingService#rejectBooking}.
     */
    public BookingResponse rejectBooking(
            Long bookingId,
            String username
    ) {

        BookingResponse response = bookingService.rejectBooking(
                bookingId,
                username
        );

        notifyOwner(response, "BOOKING_REJECTED",
                "Your booking was cancelled by the cafe. Booking ID: " + response.getId());

        return response;
    }

    /** The response carries the owner's username only, so the owner's id is read from the booking. */
    private void notifyOwner(BookingResponse response, String type, String message) {
        bookingRepository.findById(response.getId())
                .map(Booking::getUserId)
                .ifPresent(ownerId -> notificationPublisher.publish(
                        type, List.of(ownerId), message, "BOOKING", response.getId()));
    }

    // =========================
    // RESPONSE
    // =========================

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
}