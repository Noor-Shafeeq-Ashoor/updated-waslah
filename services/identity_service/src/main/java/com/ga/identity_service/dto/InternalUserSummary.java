package com.ga.identity_service.dto;

/** Minimal user data shared with other services: no email, no password, no profile. */
public record InternalUserSummary(Long id, String username, String role) {
}
