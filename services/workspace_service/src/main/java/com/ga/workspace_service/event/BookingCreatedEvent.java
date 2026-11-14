package com.ga.workspace_service.event;

public record BookingCreatedEvent(
        Long bookingId,
        String bookingOwner,
        String cafeManager
) {
}