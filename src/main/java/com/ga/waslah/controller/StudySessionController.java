
package com.ga.waslah.controller;

import com.ga.waslah.dto.CreateStudySessionRequest;
import com.ga.waslah.dto.JoinSessionResponse;
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

    @Operation(
            summary = "Join a study session",
            description = "Joins an available study session or "
                    + "adds the user to its waitlist when full."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Joined or waitlisted"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Session not found"),
            @ApiResponse(responseCode = "409", description = "Duplicate join, inactive session, or schedule conflict")
    })
    @PostMapping("/{id}/join")
    public ResponseEntity<JoinSessionResponse> joinSession(
            @PathVariable Long id,
            Authentication authentication
    ) {

        JoinSessionResponse response = studySessionService.joinSession(
                id,
                authentication.getName()
        );

        return ResponseEntity.ok(response);
    }


    @Operation(
            summary = "Leave a study session",
            description = "Removes the authenticated user from a session "
                    + "or its waitlist. If a participant leaves, "
                    + "the next eligible waitlisted user is promoted."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Successfully left session"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Session or participation not found"),
            @ApiResponse(responseCode = "409", description = "Cannot leave the session")
    })
    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leaveSession(
            @PathVariable Long id,
            Authentication authentication
    ) {

        studySessionService.leaveSession(
                id,
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }

}
