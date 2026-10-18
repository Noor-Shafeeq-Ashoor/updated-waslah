package com.ga.waslah.dto;

import com.ga.waslah.model.CafeRequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
public class CafeRegistrationDecisionRequest {

    @NotNull(message = "Decision is required")
    @Schema(description = "APPROVED or REJECTED", example = "APPROVED")
    private CafeRequestStatus decision;
}