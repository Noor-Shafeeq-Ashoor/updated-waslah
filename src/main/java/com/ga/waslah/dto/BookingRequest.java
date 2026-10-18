package com.ga.waslah.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
public class BookingRequest {

    @NotNull(message = "Study space is required")
    @Schema(description = "Id of the study space to book", example = "1")
    private Long studySpaceId;

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    @Schema(description = "Start of the slot (must be in the future)", example = "2026-12-01T14:00:00")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    @Schema(description = "End of the slot (must be after the start)", example = "2026-12-01T15:00:00")
    private LocalDateTime endTime;
}