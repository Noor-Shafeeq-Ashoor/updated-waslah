package com.ga.career_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobApplicationRequest {

    @Size( max = 1000, message = "Cover letter must not exceed 1000 characters")
    @Schema(description = "Optional cover letter, max 1000 characters", example = "I would love to join your team.")
    private String coverLetter;
}