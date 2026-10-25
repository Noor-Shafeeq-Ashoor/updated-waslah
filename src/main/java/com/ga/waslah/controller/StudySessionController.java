
package com.ga.waslah.controller;

import com.ga.waslah.dto.CreateStudySessionRequest;
import com.ga.waslah.dto.StudySessionResponse;
import com.ga.waslah.service.StudySessionService;

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

/**
 * REST controller for managing group study sessions.
 */
@Tag(
        name = "Study Sessions",
        description = "Create and manage group study sessions."
)
@RestController
@RequestMapping("/api/study-sessions")
@RequiredArgsConstructor
public class StudySessionController {

    private final StudySessionService studySessionService;
  @Operation(
            summary = "Create a study session",
            description = "Creates a group study session for an existing "
                    + "confirmed booking owned by the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Session created"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Not the booking owner"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "409", description = "Booking not confirmed, duplicate session, or capacity exceeded")
    })
    @PostMapping
    public ResponseEntity<StudySessionResponse> createSession(
            @Valid @RequestBody CreateStudySessionRequest request,
            Authentication authentication
    ) {

        StudySessionResponse response =
                studySessionService.createSession(
                        authentication.getName(),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
