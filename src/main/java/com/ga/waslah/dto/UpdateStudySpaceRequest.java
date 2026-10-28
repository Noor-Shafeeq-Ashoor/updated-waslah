package com.ga.waslah.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body used by a café manager to change a study space's name,
 * capacity or hourly price.
 */
public record UpdateStudySpaceRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @NotNull(message = "Capacity is required")
        @Min(value = 1, message = "Capacity must be at least 1")
        @Max(value = 100, message = "Capacity must not exceed 100")
        Integer capacity,

        @NotNull(message = "Price per hour is required")
        @DecimalMin(value = "0.0", message = "Price cannot be negative")
        Double pricePerHour
) {
}