package com.ga.waslah.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
public class UserProfileRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    @Schema(description = "First name", example = "Sara")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    @Schema(description = "Last name", example = "Ahmed")
    private String lastName;

    @Pattern(
            regexp = "^[0-9]{8}$",
            message = "Phone number must contain exactly 8 digits"
    )
    @Schema(description = "Exactly 8 digits", example = "33445566")
    private String phone;

    @Size(max = 500, message = "Bio must not exceed 500 characters")
    @Schema(description = "Short bio, max 500 characters", example = "Computer science student.")
    private String bio;

    @Size(max = 100, message = "Location must not exceed 100 characters")
    @Schema(description = "Location, max 100 characters", example = "Manama")
    private String location;
}