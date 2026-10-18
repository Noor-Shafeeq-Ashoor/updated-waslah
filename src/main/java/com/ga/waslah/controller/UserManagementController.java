package com.ga.waslah.controller;

import com.ga.waslah.dto.ChangePasswordRequest;
import com.ga.waslah.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;

@Tag(name = "Users & Admin", description = "Account management: password change and admin deactivation (soft delete).")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserService userService;

    /**
     * Deactivates a user account on behalf of the authenticated admin.
     *
     * @param id ID of the account to deactivate
     * @param authentication authenticated administrator's identity
     * @return HTTP 204 if the account is deactivated successfully
     */
    @Operation(
            summary = "Deactivate a user (admin)",
            description = "Soft delete: sets the account status to INACTIVE so it can no longer log in; history is preserved. Writes an audit-log entry. ADMIN only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deactivated"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated but not allowed (wrong role or not the owner)"),
            @ApiResponse(responseCode = "404", description = "Resource not found"),
            @ApiResponse(responseCode = "409", description = "User is already inactive")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateUser(
            @PathVariable Long id,
            Authentication authentication
    ) {

        userService.deactivateUser(id, authentication.getName());

        return ResponseEntity.noContent().build();
    }

    /**
     * Changes the password for the currently authenticated user.
     *
     * @param request current and new password details
     * @param authentication identity of the authenticated user
     * @return HTTP 204 if the password is changed successfully
     */
    @Operation(
            summary = "Change own password",
            description = "Requires the current password; the new password must be at least 8 characters and different from the current one."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password changed"),
            @ApiResponse(responseCode = "400", description = "Validation error, wrong current password, or new password equals current"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {

        userService.changePassword(
                authentication.getName(),
                request
        );

        return ResponseEntity.noContent().build();
    }
}
