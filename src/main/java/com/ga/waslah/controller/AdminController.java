package com.ga.waslah.controller;

import com.ga.waslah.service.UserService;
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

@Tag(name = "Users & Admin")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    /**
     * Deactivates a user account on behalf of the authenticated admin.
     *
     * @param id ID of the account to deactivate
     * @param authentication authenticated administrator's identity
     * @return HTTP 204 if the account is deactivated successfully
     */
    @Operation(
            summary = "Deactivate a user (admin route)",
            description = "Same action as DELETE /api/users/{id}, exposed on the admin route. Soft delete (status INACTIVE). ADMIN only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deactivated"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated but not allowed (wrong role or not the owner)"),
            @ApiResponse(responseCode = "404", description = "Resource not found"),
            @ApiResponse(responseCode = "409", description = "User is already inactive")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateUser(
            @PathVariable Long id,
            Authentication authentication
    ) {

        userService.deactivateUser(id, authentication.getName());

        return ResponseEntity.noContent().build();
    }
}
