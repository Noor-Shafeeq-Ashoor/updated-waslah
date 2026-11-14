
package com.ga.workspace_service.dto;

import com.ga.workspace_service.model.StudySessionStatus;

import java.time.LocalDateTime;

public record StudySessionResponse(
        Long id,
        String title,
        String description,
        Integer capacity,
        StudySessionStatus status,
        Long bookingId,
        Long studySpaceId,
        String organizerUsername,
        LocalDateTime startTime,
        LocalDateTime endTime,
        long participantCount,
        long waitlistCount


) {
}
