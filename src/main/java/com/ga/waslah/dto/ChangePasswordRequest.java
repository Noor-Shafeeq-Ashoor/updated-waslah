package com.ga.waslah.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    @Schema(description = "Current password", example = "Passw0rd!")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters")
    @Schema(description = "New password, at least 8 characters and different from the current one", example = "NewPassw0rd!")
    private String newPassword;
}

