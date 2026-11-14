package com.ga.workspace_service.dto;

import com.ga.waslah.model.CafeRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CafeRegistrationDecisionRequest {

    @NotNull(message = "Decision is required")
    @Schema(description = "APPROVED or REJECTED", example = "APPROVED")
    private CafeRequestStatus decision;
}