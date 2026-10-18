package com.ga.waslah.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
public class CafeRegistrationRequest {

    @NotBlank(message = "Cafe name is required")
    @Size(max = 55, message = "Cafe name must not exceed 55 characters")
    @Schema(description = "Name of the café to register, max 55 characters", example = "Seef Work Cafe")
    private String cafeName;

    @NotBlank(message = "Cafe description is required")
    @Size(max = 1000, message = "Cafe description must not exceed 1000 characters")
    @Schema(description = "Description, max 1000 characters", example = "Flexible workspace for freelancers.")
    private String description;

    @NotBlank(message = "Cafe location is required")
    @Schema(description = "Café location", example = "Seef, Bahrain")
    private String location;
}