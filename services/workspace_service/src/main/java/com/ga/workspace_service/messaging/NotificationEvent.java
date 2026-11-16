package com.ga.workspace_service.messaging;

import java.util.List;

/**
 * Event on topics booking-events and session-events (JSON, string key = booking or session id).
 * schemaVersion lets the format grow; unknown fields are ignored by the consumer.
 */
public record NotificationEvent(
        String eventId,
        String eventType,
        int schemaVersion,
        String occurredAt,
        List<Long> recipientUserIds,
        String message,
        String referenceType,
        Long referenceId
) {
}
