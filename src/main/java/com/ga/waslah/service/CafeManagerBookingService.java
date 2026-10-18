package com.ga.waslah.service;

import com.ga.waslah.dto.BookingResponse;
import com.ga.waslah.exception.ForbiddenException;
import com.ga.waslah.exception.ResourceNotFoundException;
import com.ga.waslah.model.Booking;
import com.ga.waslah.model.BookingStatus;
import com.ga.waslah.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CafeManagerBookingService {

    private final BookingRepository bookingRepository;
    private final AuditService auditService;
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

    public BookingResponse confirmBooking(
            Long bookingId,
            String username
    ) {

        Booking booking = getManagerBooking(
                bookingId,
                username
        );

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ForbiddenException(
                    "Only pending bookings can be confirmed"
            );
        }

        booking.setStatus(BookingStatus.CONFIRMED);

        Booking saved = bookingRepository.save(booking);

        auditService.log(
                username,
                "BOOKING_CONFIRMED",
                "BOOKING",
                saved.getId()
        );

        notificationService.sendToUser(
                saved.getUser().getUsername(),
                "BOOKING_CONFIRMED",
                "Your booking has been confirmed. Booking ID: "
                        + saved.getId()
        );

        return toResponse(saved);
    }

    // =========================
    // REJECT BOOKING
    // =========================

    public BookingResponse rejectBooking(
            Long bookingId,
            String username
    ) {

        Booking booking = getManagerBooking(
                bookingId,
                username
        );

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ForbiddenException(
                    "Only pending bookings can be rejected"
            );
        }

        booking.setStatus(BookingStatus.CANCELLED);

        Booking saved = bookingRepository.save(booking);

        auditService.log(
                username,
                "BOOKING_REJECTED",
                "BOOKING",
                saved.getId()
        );

        notificationService.sendToUser(
                saved.getUser().getUsername(),
                "BOOKING_REJECTED",
                "Your booking was rejected by the cafe. Booking ID: "
                        + saved.getId()
        );

        return toResponse(saved);
    }

    // =========================
    // CHECK MANAGER OWNERSHIP
    // =========================

    private Booking getManagerBooking(
            Long bookingId,
            String username
    ) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Booking not found"
                        ));

        boolean belongsToManager =
                booking.getStudySpace()
                        .getCafe()
                        .getManager()
                        .getUsername()
                        .equals(username);

        if (!belongsToManager) {
            throw new ForbiddenException(
                    "You are not allowed to manage this booking"
            );
        }

        return booking;
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