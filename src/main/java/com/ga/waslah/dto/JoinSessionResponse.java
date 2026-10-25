
package com.ga.waslah.dto;

public record JoinSessionResponse(
        Long sessionId,
        String status,
        String message,
        Integer waitlistPosition
) {
}
