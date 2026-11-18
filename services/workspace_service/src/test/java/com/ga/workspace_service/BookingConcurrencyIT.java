
package com.ga.workspace_service;

import com.ga.workspace_service.dto.BookingRequest;
import com.ga.workspace_service.exception.ConflictException;
import com.ga.workspace_service.model.*;
import com.ga.workspace_service.repository.*;
import com.ga.workspace_service.security.JwtPrincipal;
import com.ga.workspace_service.service.BookingService;
import com.ga.workspace_service.service.IdentityClient;
import com.ga.workspace_service.service.AuditService;
import com.ga.workspace_service.event.BookingCreatedEvent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false",
        "app.notifications.publisher=log",
        "spring.task.scheduling.enabled=false",
        "jwt.secret=test-only-secret-key-that-is-at-least-32-chars-long",
        "internal.token=test-internal-token"
})
class BookingConcurrencyIT {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name",
                () -> "org.postgresql.Driver");
    }

    @Autowired
    private BookingService bookingService;

    @Autowired
    private CafeRepository cafeRepository;

    @Autowired
    private StudySpaceRepository studySpaceRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @MockitoBean
    private IdentityClient identityClient;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private ApplicationEventPublisher eventPublisher;

    @Test
    void onlyOneUserCanBookTheSameStudySpace() throws Exception {

        when(identityClient.username(anyLong()))
                .thenAnswer(invocation ->
                        "user-" + invocation.getArgument(0));

        Cafe cafe = new Cafe();
        cafe.setName("Concurrency Test Cafe");
        cafe.setDescription("Integration test cafe");
        cafe.setLocation("Manama");
        cafe.setManagerUserId(99L);
        cafe.setActive(true);
        cafe = cafeRepository.saveAndFlush(cafe);

        StudySpace space = new StudySpace();
        space.setName("Concurrency Test Room");
        space.setCapacity(4);
        space.setPricePerHour(2.0);
        space.setAvailable(true);
        space.setCafe(cafe);
        space = studySpaceRepository.saveAndFlush(space);

        Long spaceId = space.getId();

        LocalDateTime start = LocalDateTime.now().plusDays(7);
        LocalDateTime end = start.plusHours(2);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch startSignal = new CountDownLatch(1);

        try {
            Future<Boolean> first = executor.submit(() ->
                    attemptBooking(
                            101L, spaceId, start, end,
                            ready, startSignal));

            Future<Boolean> second = executor.submit(() ->
                    attemptBooking(
                            102L, spaceId, start, end,
                            ready, startSignal));

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            startSignal.countDown();

            boolean firstSucceeded = first.get(30, TimeUnit.SECONDS);
            boolean secondSucceeded = second.get(30, TimeUnit.SECONDS);

            assertEquals(
                    1,
                    (firstSucceeded ? 1 : 0)
                            + (secondSucceeded ? 1 : 0),
                    "Exactly one booking must succeed"
            );

            List<Booking> bookings =
                    bookingRepository.findByStudySpace(space);

            long activeBookings = bookings.stream()
                    .filter(booking ->
                            booking.getStatus() == BookingStatus.PENDING
                                    || booking.getStatus() == BookingStatus.CONFIRMED)
                    .filter(booking ->
                            booking.getStartTime().isBefore(end)
                                    && booking.getEndTime().isAfter(start))
                    .count();

            assertEquals(
                    1,
                    activeBookings,
                    "PostgreSQL must contain exactly one active booking"
            );

        } finally {
            startSignal.countDown();
            executor.shutdownNow();
        }
    }

    private boolean attemptBooking(
            Long userId,
            Long spaceId,
            LocalDateTime start,
            LocalDateTime end,
            CountDownLatch ready,
            CountDownLatch startSignal
    ) throws Exception {

        JwtPrincipal principal =
                new JwtPrincipal(
                        userId,
                        "user-" + userId,
                        Role.USER
                );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of()
                )
        );

        try {
            BookingRequest request = new BookingRequest();
            request.setStudySpaceId(spaceId);
            request.setStartTime(start);
            request.setEndTime(end);

            ready.countDown();

            if (!startSignal.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Booking start signal timed out"
                );
            }

            try {
                bookingService.createBooking(
                        principal.username(),
                        request
                );
                return true;

            } catch (ConflictException exception) {
                return false;
            }

        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
