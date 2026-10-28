package com.ga.waslah.service;

import com.ga.waslah.dto.BookingResponse;
import com.ga.waslah.exception.ConflictException;
import com.ga.waslah.exception.ForbiddenException;
import com.ga.waslah.model.BookingStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The state changes (and their locking) are tested in BookingServiceTest.
 * This class only adds the customer notification, so these tests check that
 * it delegates and notifies, and that a failed change sends no notification.
 */
@ExtendWith(MockitoExtension.class)
class CafeManagerBookingServiceTest {

    @Mock
    private com.ga.waslah.repository.BookingRepository bookingRepository;

    @Mock
    private BookingService bookingService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CafeManagerBookingService service;

    private BookingResponse response(BookingStatus status) {
        return new BookingResponse(
                10L, "noor", "Bahrain Study Cafe", "Quiet Desk 1",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1),
                status, LocalDateTime.now());
    }

    @Test
    void shouldConfirmThroughLockedServiceAndNotifyCustomer() {
        when(bookingService.confirmBooking(10L, "cafe.manager"))
                .thenReturn(response(BookingStatus.CONFIRMED));

        BookingResponse result = service.confirmBooking(10L, "cafe.manager");

        assertEquals(BookingStatus.CONFIRMED, result.getStatus());
        verify(notificationService).sendToUser(
                eq("noor"), eq("BOOKING_CONFIRMED"), any());
    }

    @Test
    void shouldRejectThroughLockedServiceAndNotifyCustomer() {
        when(bookingService.rejectBooking(10L, "cafe.manager"))
                .thenReturn(response(BookingStatus.CANCELLED));

        BookingResponse result = service.rejectBooking(10L, "cafe.manager");

        assertEquals(BookingStatus.CANCELLED, result.getStatus());
        verify(notificationService).sendToUser(
                eq("noor"), eq("BOOKING_REJECTED"), any());
    }

    @Test
    void shouldNotNotifyWhenAnotherManagerTriesToConfirm() {
        when(bookingService.confirmBooking(10L, "other.manager"))
                .thenThrow(new ForbiddenException("not yours"));

        assertThrows(
                ForbiddenException.class,
                () -> service.confirmBooking(10L, "other.manager")
        );

        verify(notificationService, never()).sendToUser(any(), any(), any());
    }

    @Test
    void shouldNotNotifyWhenBookingCannotBeRejected() {
        when(bookingService.rejectBooking(10L, "cafe.manager"))
                .thenThrow(new ConflictException("already cancelled"));

        assertThrows(
                ConflictException.class,
                () -> service.rejectBooking(10L, "cafe.manager")
        );

        verify(notificationService, never()).sendToUser(any(), any(), any());
    }
}
