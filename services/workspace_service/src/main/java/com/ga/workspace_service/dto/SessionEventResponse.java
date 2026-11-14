package com.ga.workspace_service.dto;

import com.ga.workspace_service.model.SessionEvent;
import com.ga.workspace_service.model.SessionEventType;

import java.time.LocalDateTime;

/** One entry of a study session's history. */
public record SessionEventResponse(
        Long id,
        SessionEventType eventType,
        String username,
        String details,
        LocalDateTime occurredAt
) {
    public static SessionEventResponse from(SessionEvent e, String username) {
        return new SessionEventResponse(
                e.getId(), e.getEventType(),
                username,
                e.getDetails(), e.getOccurredAt());
    }
}
