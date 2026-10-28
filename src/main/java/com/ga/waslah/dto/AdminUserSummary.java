package com.ga.waslah.dto;

import com.ga.waslah.model.Role;
import com.ga.waslah.model.User;
import com.ga.waslah.model.UserStatus;

/** One row of the admin user list. Never contains the password hash. */
public record AdminUserSummary(
        Long id,
        String username,
        String email,
        Role role,
        UserStatus status,
        boolean emailVerified,
        String firstName,
        String lastName
) {
    public static AdminUserSummary from(User user) {
        return new AdminUserSummary(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.isEmailVerified(),
                user.getUserProfile() == null ? null : user.getUserProfile().getFirstName(),
                user.getUserProfile() == null ? null : user.getUserProfile().getLastName());
    }
}