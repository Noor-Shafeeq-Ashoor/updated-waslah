package com.ga.waslah.controller;

import com.ga.waslah.dto.CafeRegistrationDecisionRequest;
import com.ga.waslah.dto.CafeRegistrationRequest;
import com.ga.waslah.dto.CafeRegistrationResponse;
import com.ga.waslah.service.CafeRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Role Upgrade Requests")
@RestController
@RequestMapping("/api/cafe-requests")
public class CafeRegistrationController {

    private final CafeRegistrationService cafeRegistrationService;

    public CafeRegistrationController(
            CafeRegistrationService cafeRegistrationService
    ) {
        this.cafeRegistrationService = cafeRegistrationService;
    }

    // =========================
    // USER - SUBMIT REQUEST
    // =========================

    @Operation(
            summary = "Request to register a café (become Café Manager)",
            description = "USER role only. An ADMIN must approve before the role changes to CAFE_MANAGER."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Request submitted (PENDING)"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid request"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated but not allowed (wrong role or not the owner)")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CafeRegistrationResponse submitRequest(
            Authentication authentication,
            @Valid @RequestBody CafeRegistrationRequest request
    ) {

        return cafeRegistrationService.submitRequest(
                authentication.getName(),
                request
        );
    }

    // =========================
    // USER - GET MY REQUESTS
    // =========================

    @Operation(
            summary = "List my café requests",
            description = "Requests submitted by the caller."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of requests"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT")
    })
    @GetMapping("/my")
    public List<CafeRegistrationResponse> getMyRequests(
            Authentication authentication
    ) {

        return cafeRegistrationService.getMyRequests(
                authentication.getName()
        );
    }

    // =========================
    // ADMIN - GET PENDING REQUESTS
    // =========================

    @Operation(
            summary = "List pending café requests (admin)",
            description = "ADMIN only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of pending requests"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated but not allowed (wrong role or not the owner)")
    })
    @GetMapping("/pending")
    public List<CafeRegistrationResponse> getPendingRequests(
            Authentication authentication
    ) {

        return cafeRegistrationService.getPendingRequests(
                authentication.getName()
        );
    }

    // =========================
    // ADMIN - APPROVE / REJECT
    // =========================

    @Operation(
            summary = "Approve or reject a café request (admin)",
            description = "Body: {\"decision\": \"APPROVED\"} or {\"decision\": \"REJECTED\"}. Approval makes the user a CAFE_MANAGER and creates the café. ADMIN only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Decision recorded"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid request"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin, or the request was already processed"),
            @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    @PutMapping("/{id}/decision")
    public CafeRegistrationResponse decideRequest(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody CafeRegistrationDecisionRequest request
    ) {

        return cafeRegistrationService.decideRequest(
                id,
                authentication.getName(),
                request
        );
    }
}