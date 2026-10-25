
package com.ga.waslah.dto;

import com.ga.waslah.model.StudySessionStatus;
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
        LocalDateTime endTime
) {
}
