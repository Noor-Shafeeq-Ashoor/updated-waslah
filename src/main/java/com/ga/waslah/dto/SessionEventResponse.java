package com.ga.waslah.dto;

import com.ga.waslah.model.SessionEvent;
import com.ga.waslah.model.SessionEventType;

import java.time.LocalDateTime;

/** One entry of a study session's history. */
public record SessionEventResponse(
        Long id,
        SessionEventType eventType,
        String username,
        String details,
        LocalDateTime occurredAt
) {
    public static SessionEventResponse from(SessionEvent e) {
        return new SessionEventResponse(
                e.getId(), e.getEventType(),
                e.getUser() == null ? null : e.getUser().getUsername(),
                e.getDetails(), e.getOccurredAt());
    }
}
