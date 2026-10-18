package com.ga.waslah.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record ResetPasswordRequest(

        @NotBlank(message = "Token is required")
        @Schema(description = "Reset token from the email", example = "<token-from-email>") String token,

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Schema(description = "New password, at least 8 characters", example = "NewPassw0rd!") String newPassword

) {
}