package com.ga.waslah.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

public record ForgotPasswordRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Schema(description = "Email of the account", example = "sara@example.com") String email

) {
}