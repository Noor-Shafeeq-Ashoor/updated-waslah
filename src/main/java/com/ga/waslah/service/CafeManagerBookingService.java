package com.ga.waslah.service;

import com.ga.waslah.dto.BookingResponse;
import com.ga.waslah.model.Booking;
import com.ga.waslah.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CafeManagerBookingService {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final NotificationService notificationService;

    // =========================
    // GET MY CAFE BOOKINGS
    // =========================

    public List<BookingResponse> getMyCafeBookings(String username) {

        return bookingRepository
                .findByStudySpace_Cafe_Manager_Username(username)
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

        notificationService.sendToUser(
                response.getUsername(),
                "BOOKING_CONFIRMED",
                "Your booking has been confirmed. Booking ID: "
                        + response.getId()
        );

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

        notificationService.sendToUser(
                response.getUsername(),
                "BOOKING_REJECTED",
                "Your booking was cancelled by the cafe. Booking ID: "
                        + response.getId()
        );

        return response;
    }

    // =========================
    // RESPONSE
    // =========================

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