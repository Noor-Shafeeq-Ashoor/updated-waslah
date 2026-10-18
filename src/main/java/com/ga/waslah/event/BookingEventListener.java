
package com.ga.waslah.event;

import com.ga.waslah.service.NotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Handles booking events after the associated transaction commits.
 */
@Component
public class BookingEventListener {

    private final NotificationService notificationService;

    /**
     * Creates the listener with the required notification service.
     *
     * @param notificationService service used to deliver notifications
     */
    public BookingEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Notifies the booking owner and cafe manager after a booking
     * has been committed successfully.
     *
     * @param event the event containing the booking and recipient details
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingCreated(BookingCreatedEvent event) {
        String message = "A new booking has been created. Booking ID: "
                + event.bookingId();

        notificationService.sendToUser(
                event.bookingOwner(),
                "BOOKING_CREATED",
                message
        );

        if (!event.cafeManager().equals(event.bookingOwner())) {
            notificationService.sendToUser(
                    event.cafeManager(),
                    "BOOKING_CREATED",
                    message
            );
        }
    }
}
