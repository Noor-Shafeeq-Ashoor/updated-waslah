package com.ga.notification_service.security;

import com.ga.notification_service.model.Role;

public record JwtPrincipal(Long id, String username, Role role) {

    @Override
    public String toString() {
        return username;
    }
}
