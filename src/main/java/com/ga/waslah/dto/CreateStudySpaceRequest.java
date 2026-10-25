
package com.ga.waslah.dto;

import jakarta.validation.constraints.*;

/**
 * Request body used by a café manager to create a study space.
 */
public record CreateStudySpaceRequest(

        @NotBlank(message = "Name is required")
        String name,

        @NotNull
        @Min(value = 1, message = "Capacity must be at least 1")
        Integer capacity,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = true)
        Double pricePerHour
) {
}
