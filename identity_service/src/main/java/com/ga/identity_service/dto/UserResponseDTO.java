package com.ga.identity_service.dto;

import com.ga.identity_service.model.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UserResponseDTO {

    private String username;
    private String email;
    private Role role;
}