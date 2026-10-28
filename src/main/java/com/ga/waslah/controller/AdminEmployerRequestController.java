package com.ga.waslah.controller;

import com.ga.waslah.dto.EmployerRequestResponse;

import com.ga.waslah.model.EmployerRequest;
import com.ga.waslah.service.EmployerRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Role Upgrade Requests")
@RestController
@RequestMapping("/api/admin/employer-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminEmployerRequestController {

    private final EmployerRequestService employerRequestService;

    @Operation(
            summary = "List employer requests (admin)",
            description = "Optional query parameter status = PENDING (default) | APPROVED | REJECTED | ALL. ADMIN only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of requests with the chosen status"),
            @ApiResponse(responseCode = "400", description = "Unknown status value"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated but not allowed (wrong role or not the owner)")
    })
    @GetMapping
    public ResponseEntity<List<EmployerRequestResponse>> getPendingRequests(
            @RequestParam(defaultValue = "PENDING") String status
    ) {

        List<EmployerRequestResponse> response =
                employerRequestService.getRequests(
                                "ALL".equalsIgnoreCase(status)
                                        ? null
                                        : com.ga.waslah.model.EmployerRequestStatus
                                        .valueOf(status.toUpperCase()))
                        .stream()
                        .map(this::toDTO)
                        .toList();

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Approve an employer request (admin)",
            description = "The requesting user's role becomes EMPLOYER. Writes an audit-log entry. ADMIN only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request approved"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated but not allowed (wrong role or not the owner)"),
            @ApiResponse(responseCode = "404", description = "Resource not found"),
            @ApiResponse(responseCode = "409", description = "Conflict with the current state of the resource")
    })
    @PutMapping("/{id}/approve")
    public ResponseEntity<EmployerRequestResponse> approveRequest(
            @PathVariable Long id,
            Authentication authentication
    ) {
        EmployerRequest request =
                employerRequestService.approveRequest(
                        id,
                        authentication.getName()
                );

        return ResponseEntity.ok(toDTO(request));
    }

    @Operation(
            summary = "Reject an employer request (admin)",
            description = "ADMIN only. Writes an audit-log entry."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request rejected"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated but not allowed (wrong role or not the owner)"),
            @ApiResponse(responseCode = "404", description = "Resource not found"),
            @ApiResponse(responseCode = "409", description = "Conflict with the current state of the resource")
    })
    @PutMapping("/{id}/reject")
    public ResponseEntity<EmployerRequestResponse> rejectRequest(
            @PathVariable Long id,
            Authentication authentication
    ) {
        EmployerRequest request =
                employerRequestService.rejectRequest(
                        id,
                        authentication.getName()
                );

        return ResponseEntity.ok(toDTO(request));
    }

    private EmployerRequestResponse toDTO(EmployerRequest request) {

        return new EmployerRequestResponse(
                request.getId(),
                request.getCompanyName(),
                request.getCompanyDescription(),
                request.getJobTypes(),
                request.getCommercialRegistration(),
                request.getStatus(),
                request.getUser() == null ? null : request.getUser().getUsername()
        );
    }
}