package com.ga.waslah.dto;

import com.ga.waslah.model.Role;
import com.ga.waslah.model.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class AdminUserResponseDTO {

    private String username;
    private String email;
    private Role role;
    private UserProfile profile;
}