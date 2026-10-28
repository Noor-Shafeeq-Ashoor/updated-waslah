package com.ga.waslah.dto;

import com.ga.waslah.model.Notification;

import java.time.LocalDateTime;

/** A stored notification as returned by GET /api/notifications. */
public record NotificationResponse(
        Long id,
        String type,
        String message,
        String referenceType,
        Long referenceId,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType(), n.getMessage(),
                n.getReferenceType(), n.getReferenceId(),
                n.isRead(), n.getCreatedAt());
    }
}
