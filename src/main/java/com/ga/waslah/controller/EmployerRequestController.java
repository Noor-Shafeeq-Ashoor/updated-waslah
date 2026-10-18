package com.ga.waslah.controller;

import com.ga.waslah.dto.EmployerRequestRequest;
import com.ga.waslah.dto.EmployerRequestResponse;
import com.ga.waslah.model.EmployerRequest;
import com.ga.waslah.service.EmployerRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Role Upgrade Requests", description = "Users ask to become Employers or Café Managers; an admin decides.")
@RestController
@RequestMapping("/api/employer-requests")
@RequiredArgsConstructor
public class EmployerRequestController {

    private final EmployerRequestService employerRequestService;

    private EmployerRequestResponse toResponse(
            EmployerRequest employerRequest
    ) {

        return new EmployerRequestResponse(
                employerRequest.getCompanyName(),
                employerRequest.getCompanyDescription(),
                employerRequest.getJobTypes(),
                employerRequest.getCommercialRegistration(),
                employerRequest.getStatus()
        );
    }

    @Operation(
            summary = "Request to become an Employer",
            description = "USER role only; only one open request at a time. An ADMIN must approve it before the role changes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Request submitted (PENDING)"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid request"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated but not allowed (wrong role or not the owner)"),
            @ApiResponse(responseCode = "409", description = "Conflict with the current state of the resource")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResponseEntity<EmployerRequestResponse> createRequest(
            @Valid @RequestBody EmployerRequestRequest request,
            Authentication authentication
    ) {

        EmployerRequest employerRequest =
                employerRequestService.createRequest(
                        authentication.getName(),
                        request
                );

        return new ResponseEntity<>(
                toResponse(employerRequest),
                HttpStatus.CREATED
        );
    }

    @Operation(
            summary = "View my employer request",
            description = "Returns the caller's employer request."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employer request"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    @GetMapping("/my")
    public ResponseEntity<EmployerRequestResponse> getMyRequest(
            Authentication authentication
    ) {

        EmployerRequest employerRequest =
                employerRequestService.getMyRequest(
                        authentication.getName()
                );

        return ResponseEntity.ok(
                toResponse(employerRequest)
        );
    }
}
