
package com.ga.waslah.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body used to enable or disable a study space.
 */
public record UpdateStudySpaceAvailabilityRequest(

        @NotNull(message = "Availability is required")
        Boolean available

) {
}
