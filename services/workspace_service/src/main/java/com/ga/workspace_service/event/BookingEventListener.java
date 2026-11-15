package com.ga.workspace_service.event;


import com.ga.workspace_service.service.NotificationPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles booking events asynchronously after the associated
 * database transaction has committed successfully.
 */
@Slf4j
@Component
public class BookingEventListener {

    private final NotificationPublisher notificationPublisher;

    public BookingEventListener(NotificationPublisher notificationPublisher) {
        this.notificationPublisher = notificationPublisher;
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingCreated(BookingCreatedEvent event) {
        log.info("Processing booking notification for booking {} on thread {}",
                event.bookingId(), Thread.currentThread().getName());

        // Owner and manager, without a duplicate when they are the same person.
        Set<Long> recipients = new LinkedHashSet<>();
        recipients.add(event.bookingOwnerId());
        recipients.add(event.cafeManagerId());

        notificationPublisher.publish(
                "BOOKING_CREATED",
                List.copyOf(recipients),
                "A new booking has been created. Booking ID: " + event.bookingId(),
                "BOOKING",
                event.bookingId()
        );
    }
}