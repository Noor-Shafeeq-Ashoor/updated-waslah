package com.ga.waslah.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
public class CafeRequest {

    @NotBlank(message = "Cafe name is required")
    @Size(max = 55, message = "Cafe name must not exceed 55 characters")
    @Schema(description = "Café name, max 55 characters", example = "Bahrain Study Cafe")
    private String name;

    @NotBlank(message = "Cafe description is required")
    @Size(max = 1000, message = "Cafe description must not exceed 1000 characters")
    @Schema(description = "Description, max 1000 characters", example = "A quiet cafe with desks and group rooms.")
    private String description;

    @NotBlank(message = "Cafe location is required")
    @Schema(description = "Café location", example = "Manama, Bahrain")
    private String location;
}