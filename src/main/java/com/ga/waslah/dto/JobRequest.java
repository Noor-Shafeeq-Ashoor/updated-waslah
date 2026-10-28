package com.ga.waslah.dto;

import com.ga.waslah.model.JobType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
public class JobRequest {

    @NotBlank(message = "Title is required")
    @Size(
            max = 150,
            message = "Title must not exceed 150 characters"
    )
    @Schema(description = "Job title", example = "Junior Java Developer")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(
            max = 2000,
            message = "Description must not exceed 2000 characters"
    )
    @Schema(description = "Job description", example = "Build and maintain REST APIs with Spring Boot.")
    private String description;

    @NotBlank(message = "Company name is required")
    @Size(
            max = 150,
            message = "Company name must not exceed 150 characters"
    )
    @Schema(description = "Hiring company", example = "Gulf Tech")
    private String companyName;

    @NotBlank(message = "Location is required")
    @Size(
            max = 150,
            message = "Location must not exceed 150 characters"
    )
    @Schema(description = "Job location", example = "Manama, Bahrain")
    private String location;

    @NotNull(message = "Job type is required")
    @Schema(description = "FULL_TIME, PART_TIME, CONTRACT, INTERNSHIP or FREELANCE", example = "FULL_TIME")
    private JobType jobType;

    @PositiveOrZero(message = "Salary cannot be negative")
    @Schema(description = "Monthly salary (optional, not negative)", example = "700")
    private Double salary;

    @NotNull(message = "Deadline is required")
    @Future(message = "Deadline must be in the future")
    @Schema(description = "Application deadline (must be in the future)", example = "2026-12-31T23:59:00")
    private LocalDateTime deadline;

    @Min(value = 1, message = "Vacancies must be at least 1")
    @Max(value = 1000, message = "Vacancies must not exceed 1000")
    @Schema(description = "Number of open positions (optional, default 1)", example = "3")
    private Integer vacancies;



}