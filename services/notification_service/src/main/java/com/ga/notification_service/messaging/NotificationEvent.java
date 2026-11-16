package com.ga.notification_service.messaging;

import java.util.List;

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
