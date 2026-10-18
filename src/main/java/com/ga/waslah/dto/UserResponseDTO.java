package com.ga.waslah.dto;

import com.ga.waslah.model.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UserResponseDTO {

    private String username;
    private String email;
    private Role role;
}