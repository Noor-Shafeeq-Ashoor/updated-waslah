package com.ga.identity_service.dto;

import com.ga.identity_service.model.Role;
import com.ga.identity_service.model.UserProfile;
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