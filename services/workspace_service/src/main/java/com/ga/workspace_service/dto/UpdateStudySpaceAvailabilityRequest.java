
package com.ga.workspace_service.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body used to enable or disable a study space.
 */
public record UpdateStudySpaceAvailabilityRequest(

        @NotNull(message = "Availability is required")
        Boolean available

) {
}
