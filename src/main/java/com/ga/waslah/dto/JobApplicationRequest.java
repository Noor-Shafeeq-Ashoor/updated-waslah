package com.ga.waslah.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
public class JobApplicationRequest {

    @Size( max = 1000, message = "Cover letter must not exceed 1000 characters")
    @Schema(description = "Optional cover letter, max 1000 characters", example = "I would love to join your team.")
    private String coverLetter;
}