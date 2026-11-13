
package com.ga.identity_service.controller;

import com.ga.identity_service.dto.EmployerRequestRequest;
import com.ga.identity_service.dto.EmployerRequestResponse;
import com.ga.identity_service.model.EmployerRequest;
import com.ga.identity_service.service.EmployerRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@Tag(
        name = "Role Upgrade Requests",
        description = "Users ask to become Employers or Café Managers; an admin decides."
)
@RestController
@RequestMapping("/api/employer-requests")
@RequiredArgsConstructor
public class EmployerRequestController {

    private final EmployerRequestService employerRequestService;

    @Operation(
            summary = "Request to become an Employer",
            description = "USER role only; only one open request at a time. "
                    + "An ADMIN must approve it before the role changes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Request submitted (PENDING)"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid request"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated but not allowed (wrong role or not the owner)"),
            @ApiResponse(responseCode = "409", description = "Conflict with the current state of the resource")
    })
    @PostMapping
    public ResponseEntity<EmployerRequestResponse> createRequest(
            @Valid @RequestBody EmployerRequestRequest request,
            Authentication authentication
    ) {

        // Create and save the employer request.
        EmployerRequest employerRequest =
                employerRequestService.createRequest(
                        authentication.getName(),
                        request
                );

        // Include the generated ID in the response.
        EmployerRequestResponse response =
                employerRequestService.toResponse(employerRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
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

        EmployerRequest employerRequest = employerRequestService.getMyRequest(authentication.getName());

        // Reuse the same mapper to keep API responses consistent.
        return ResponseEntity.ok(employerRequestService.toResponse(employerRequest)
        );
    }
}
