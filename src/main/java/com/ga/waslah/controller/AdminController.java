package com.ga.waslah.controller;

import com.ga.waslah.dto.AdminStatsResponse;
import com.ga.waslah.dto.AdminUserSummary;
import com.ga.waslah.dto.PageResponse;
import com.ga.waslah.model.Role;
import com.ga.waslah.model.UserStatus;
import com.ga.waslah.service.AdminDashboardService;
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
    private final AdminDashboardService adminDashboardService;

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

    @Operation(
            summary = "List users (admin)",
            description = "Paginated list for the admin dashboard. Optional filters: role, status (ACTIVE/INACTIVE), keyword (matches username or email). Pagination: page (0-based), size (1-100, default 10). Sort: id | username | email | role | status. ADMIN only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of users"),
            @ApiResponse(responseCode = "400", description = "Invalid filter, page, size, sort field or direction"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Not an admin")
    })
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<AdminUserSummary>> listUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction
    ) {
        return ResponseEntity.ok(
                userService.listUsers(role, status, keyword, page, size, sort, direction));
    }

    @Operation(
            summary = "Reactivate a user (admin)",
            description = "Sets a deactivated account (INACTIVE) back to ACTIVE and clears any login lock. Writes an audit-log entry. ADMIN only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User reactivated"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Not an admin"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "User is already active")
    })
    @PutMapping("/users/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reactivateUser(
            @PathVariable Long id,
            Authentication authentication
    ) {
        userService.reactivateUser(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Dashboard numbers (admin)",
            description = "Counts of users (by role and status), pending employer and café requests, open jobs, pending and confirmed bookings and active study sessions. ADMIN only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard statistics"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Not an admin")
    })
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(adminDashboardService.getStats());
    }
}
