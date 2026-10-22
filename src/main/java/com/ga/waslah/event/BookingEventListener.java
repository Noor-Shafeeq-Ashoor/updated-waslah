package com.ga.waslah.event;

import com.ga.waslah.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Handles booking events asynchronously after the associated
 * database transaction has committed successfully.
 */
@Slf4j
@Component
public class BookingEventListener {

    private final NotificationService notificationService;

    public BookingEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Sends booking notifications asynchronously to the booking owner
     * and cafe manager after the booking transaction commits.
     */
    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)

    public void handleBookingCreated(BookingCreatedEvent event) {
        log.info("BOOKING NOTIFICATION - Running on thread: {}",
                Thread.currentThread().getName());

        // Log the thread processing the notification.
        log.info(
                "Processing booking notification for booking {} on thread {}",
                event.bookingId(),
                Thread.currentThread().getName()
        );

        String message = "A new booking has been created. Booking ID: "
                + event.bookingId();

        // Notify the user who created the booking.
        notificationService.sendToUser(
                event.bookingOwner(),
                "BOOKING_CREATED",
                message
        );

        // Notify the cafe manager, avoiding duplicate notifications.
        if (!event.cafeManager().equals(event.bookingOwner())) {
            notificationService.sendToUser(
                    event.cafeManager(),
                    "BOOKING_CREATED",
                    message
            );
        }

        log.info(
                "Finished processing notifications for booking {}",
                event.bookingId()
        );
    }
}