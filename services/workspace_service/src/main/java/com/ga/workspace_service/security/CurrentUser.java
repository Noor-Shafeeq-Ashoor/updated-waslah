package com.ga.workspace_service.security;

import com.ga.workspace_service.model.Role;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public JwtPrincipal get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtPrincipal principal) {
            return principal;
        }
        throw new AuthenticationCredentialsNotFoundException("Authentication is required");
    }

    public Long id() {
        return get().id();
    }

    public String username() {
        return get().username();
    }

    public Role role() {
        return get().role();
    }
}
