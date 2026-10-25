
package com.ga.waslah.dto;

import jakarta.validation.constraints.*;

public record CreateStudySessionRequest(

        @NotNull(message = "Booking ID is required")
        Long bookingId,

        @NotBlank(message = "Session title is required")
        @Size(max = 150)
        String title,

        @Size(max = 1000)
        String description,

        @NotNull(message = "Capacity is required")
        @Min(value = 1, message = "Capacity must be at least 1")
        Integer capacity

) {
}
