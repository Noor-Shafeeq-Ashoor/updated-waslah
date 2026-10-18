package com.ga.waslah.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
public class EmployerRequestRequest {

    @NotBlank(message = "Company name is required")
    @Size(max = 150, message = "Company name must not exceed 150 characters")
    @Schema(description = "Company name", example = "Gulf Tech")
    private String companyName;

    @NotBlank(message = "Company description is required")
    @Size(max = 1000, message = "Company description must not exceed 1000 characters")
    @Schema(description = "Company description, max 1000 characters", example = "Software consultancy.")
    private String companyDescription;

    @NotBlank(message = "Job types are required")
    @Schema(description = "Types of jobs the company will post", example = "FULL_TIME, INTERNSHIP")
    private String jobTypes;

    @NotBlank(message = "Commercial registration is required")
    @Schema(description = "Commercial registration number", example = "CR-123456")
    private String commercialRegistration;
}