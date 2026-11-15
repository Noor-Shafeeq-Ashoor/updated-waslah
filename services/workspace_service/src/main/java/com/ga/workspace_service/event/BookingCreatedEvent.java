package com.ga.workspace_service.event;

// Published after a booking is saved; carries user ids (users live in identity-service).
public record BookingCreatedEvent(
        Long bookingId,
        Long bookingOwnerId,
        Long cafeManagerId
) {
}