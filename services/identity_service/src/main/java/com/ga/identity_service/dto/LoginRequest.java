package com.ga.identity_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @Schema(description = "Account username", example = "sara")
    private String username;
    @Schema(description = "Account password", example = "Passw0rd!")
    private String password;
}