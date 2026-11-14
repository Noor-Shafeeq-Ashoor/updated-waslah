
package com.ga.workspace_service.dto;

public record JoinSessionResponse(
        Long sessionId,
        String status,
        String message,
        Integer waitlistPosition
) {
}
