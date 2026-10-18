package com.ga.waslah.service;

import com.ga.waslah.dto.BookingResponse;
import com.ga.waslah.exception.ForbiddenException;
import com.ga.waslah.model.Booking;
import com.ga.waslah.model.BookingStatus;
import com.ga.waslah.model.Cafe;
import com.ga.waslah.model.StudySpace;
import com.ga.waslah.model.User;
import com.ga.waslah.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CafeManagerBookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CafeManagerBookingService service;

    private Booking booking;

    @BeforeEach
    void setUp() {
        User manager = new User();
        manager.setUsername("cafe.manager");

        Cafe cafe = new Cafe();
        cafe.setName("Bahrain Study Cafe");
        cafe.setManager(manager);

        StudySpace space = new StudySpace();
        space.setName("Quiet Desk 1");
        space.setCafe(cafe);

        User customer = new User();
        customer.setUsername("noor");

        booking = new Booking();
        booking.setId(10L);
        booking.setUser(customer);
        booking.setStudySpace(space);
        booking.setStartTime(LocalDateTime.now().plusDays(1));
        booking.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        booking.setStatus(BookingStatus.PENDING);
    }

    @Test
    void shouldConfirmPendingBookingAndNotifyCustomer() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        BookingResponse response = service.confirmBooking(10L, "cafe.manager");

        assertEquals(BookingStatus.CONFIRMED, response.getStatus());
        verify(auditService).log("cafe.manager", "BOOKING_CONFIRMED", "BOOKING", 10L);
        verify(notificationService).sendToUser(
                org.mockito.ArgumentMatchers.eq("noor"),
                org.mockito.ArgumentMatchers.eq("BOOKING_CONFIRMED"),
                any());
    }

    @Test
    void shouldRejectPendingBookingAndFreeTheSlot() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        BookingResponse response = service.rejectBooking(10L, "cafe.manager");

        assertEquals(BookingStatus.CANCELLED, response.getStatus());
        verify(auditService).log("cafe.manager", "BOOKING_REJECTED", "BOOKING", 10L);
    }

    @Test
    void shouldNotConfirmBookingThatIsNotPending() {
        booking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        assertThrows(
                ForbiddenException.class,
                () -> service.confirmBooking(10L, "cafe.manager")
        );

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldNotLetAnotherManagerConfirmBooking() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        assertThrows(
                ForbiddenException.class,
                () -> service.confirmBooking(10L, "other.manager")
        );

        verify(bookingRepository, never()).save(any());
    }
}
