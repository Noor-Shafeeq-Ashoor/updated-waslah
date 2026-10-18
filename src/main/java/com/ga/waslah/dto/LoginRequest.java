package com.ga.waslah.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

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