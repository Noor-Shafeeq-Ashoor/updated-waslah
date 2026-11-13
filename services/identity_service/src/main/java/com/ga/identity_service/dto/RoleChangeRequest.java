package com.ga.identity_service.dto;

import com.ga.identity_service.model.Role;
import jakarta.validation.constraints.NotNull;

public record RoleChangeRequest(@NotNull Role role) {
}
