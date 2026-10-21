package com.ga.waslah.service;

import com.ga.waslah.dto.BookingRequest;
import com.ga.waslah.dto.BookingResponse;
import com.ga.waslah.event.BookingCreatedEvent;
import com.ga.waslah.exception.ForbiddenException;
import com.ga.waslah.model.Booking;
import com.ga.waslah.model.BookingStatus;
import com.ga.waslah.model.Cafe;
import com.ga.waslah.model.StudySpace;
import com.ga.waslah.model.User;
import com.ga.waslah.repository.BookingRepository;
import com.ga.waslah.repository.StudySpaceRepository;
import com.ga.waslah.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private StudySpaceRepository studySpaceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BookingService bookingService;

    private User user;
    private StudySpace space;
    private final LocalDateTime start = LocalDateTime.now().plusDays(1);
    private final LocalDateTime end = start.plusHours(1);

    @BeforeEach
    void setUp() {
        User manager = new User();
        manager.setUsername("cafe.manager");

        Cafe cafe = new Cafe();
        cafe.setName("Bahrain Study Cafe");
        cafe.setManager(manager);

        space = new StudySpace();
        space.setId(1L);
        space.setName("Quiet Desk 1");
        space.setAvailable(true);
        space.setCafe(cafe);

        user = new User();
        user.setUsername("noor");
    }

    private BookingRequest request(LocalDateTime from, LocalDateTime to) {
        BookingRequest request = new BookingRequest();
        request.setStudySpaceId(1L);
        request.setStartTime(from);
        request.setEndTime(to);
        return request;
    }

    private Booking bookingOwnedBy(User owner, BookingStatus status) {
        Booking booking = new Booking();
        booking.setId(10L);
        booking.setUser(owner);
        booking.setStudySpace(space);
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setStatus(status);
        return booking;
    }

    // ---------------------------------------------------------------- create

    @Test
    void shouldCreatePendingBookingAndPublishEvent() {
        when(userRepository.findByUsername("noor")).thenReturn(Optional.of(user));
        when(studySpaceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(space));
        when(bookingRepository
                .existsByStudySpaceAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                        eq(space), anyList(), eq(end), eq(start)))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        BookingResponse response =
                bookingService.createBooking("noor", request(start, end));

        assertEquals(BookingStatus.PENDING, response.getStatus());
        assertEquals("noor", response.getUsername());
        verify(eventPublisher).publishEvent(any(BookingCreatedEvent.class));
        verify(auditService).log("noor", "BOOKING_CREATED", "BOOKING", 10L);
    }

    @Test
    void shouldRejectOverlappingBooking() {
        when(userRepository.findByUsername("noor")).thenReturn(Optional.of(user));
        when(studySpaceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(space));
        when(bookingRepository

                .existsByStudySpaceAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                        eq(space), anyList(), eq(end), eq(start)))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking("noor", request(start, end))
        );

        verify(bookingRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldRejectEndTimeBeforeStartTime() {
        when(userRepository.findByUsername("noor")).thenReturn(Optional.of(user));
        when(studySpaceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(space));

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking("noor", request(end, start))
        );

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldRejectBookingForUnavailableSpace() {
        space.setAvailable(false);
        when(userRepository.findByUsername("noor")).thenReturn(Optional.of(user));
        when(studySpaceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(space));

        assertThrows(
                ForbiddenException.class,
                () -> bookingService.createBooking("noor", request(start, end))
        );

        verify(bookingRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- cancel

    @Test
    void shouldCancelOwnPendingBooking() {
        Booking booking = bookingOwnedBy(user, BookingStatus.PENDING);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        BookingResponse response = bookingService.cancelBooking(10L, "noor");

        assertEquals(BookingStatus.CANCELLED, response.getStatus());
        verify(auditService).log("noor", "BOOKING_CANCELLED", "BOOKING", 10L);
    }

    @Test
    void shouldNotCancelAnotherUsersBooking() {
        User other = new User();
        other.setUsername("sara");
        Booking booking = bookingOwnedBy(other, BookingStatus.PENDING);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        assertThrows(
                ForbiddenException.class,
                () -> bookingService.cancelBooking(10L, "noor")
        );

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldNotCancelCompletedBooking() {
        Booking booking = bookingOwnedBy(user, BookingStatus.COMPLETED);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        assertThrows(
                ForbiddenException.class,
                () -> bookingService.cancelBooking(10L, "noor")
        );

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldNotCancelAlreadyCancelledBooking() {
        Booking booking = bookingOwnedBy(user, BookingStatus.CANCELLED);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        assertThrows(
                ForbiddenException.class,
                () -> bookingService.cancelBooking(10L, "noor")
        );

        verify(bookingRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- view

    @Test
    void shouldNotShowAnotherUsersBooking() {
        User other = new User();
        other.setUsername("sara");
        when(bookingRepository.findById(10L))
                .thenReturn(Optional.of(bookingOwnedBy(other, BookingStatus.PENDING)));

        assertThrows(
                ForbiddenException.class,
                () -> bookingService.getBookingById(10L, "noor")
        );
    }
}
