package com.ga.waslah.event;

public record BookingCreatedEvent(
        Long bookingId,
        String bookingOwner,
        String cafeManager
) {
}