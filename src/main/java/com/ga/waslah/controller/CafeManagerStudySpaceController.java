
package com.ga.waslah.controller;

import com.ga.waslah.dto.*;
import com.ga.waslah.service.CafeManagerStudySpaceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Allows café managers to manage study spaces in their own cafés.
 */
@RestController
@RequestMapping("/api/cafes/{cafeId}/study-spaces")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CAFE_MANAGER')")
@Tag(
        name = "Café Manager Study Spaces",
        description = "Manage study spaces and their availability"
)
public class CafeManagerStudySpaceController {

    private final CafeManagerStudySpaceService studySpaceService;

    /**
     * Creates a new study space for the manager's café.
     */
    @PostMapping
    @Operation(summary = "Create a study space")
    public ResponseEntity<StudySpaceResponse> createStudySpace(
            @PathVariable Long cafeId,
            @Valid @RequestBody CreateStudySpaceRequest request,
            Authentication authentication
    ) {

        StudySpaceResponse response =
                studySpaceService.createStudySpace(
                        cafeId,
                        request,
                        authentication.getName()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Lists all study spaces of the manager's café, including unavailable ones.
     */
    @GetMapping
    @Operation(
            summary = "List all study spaces of my café",
            description = "Includes spaces that are currently unavailable. Only the café's own manager."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All study spaces of the café"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Not a café manager, or not your café"),
            @ApiResponse(responseCode = "404", description = "Café not found")
    })
    public ResponseEntity<List<StudySpaceResponse>> listStudySpaces(
            @PathVariable Long cafeId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                studySpaceService.listStudySpaces(cafeId, authentication.getName()));
    }

    /**
     * Changes name, capacity and price of a study space.
     */
    @PutMapping("/{spaceId}")
    @Operation(
            summary = "Edit a study space",
            description = "Changes name, capacity and price. Runs under the shared participation lock, so it cannot race with session creation. Capacity cannot be set below the size of a session that is still OPEN or FULL in this space."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Study space updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Not a café manager, or not your café"),
            @ApiResponse(responseCode = "404", description = "Study space not found in this café"),
            @ApiResponse(responseCode = "409", description = "Capacity is lower than a session still running in this space")
    })
    public ResponseEntity<StudySpaceResponse> updateStudySpace(
            @PathVariable Long cafeId,
            @PathVariable Long spaceId,
            @Valid @RequestBody UpdateStudySpaceRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                studySpaceService.updateStudySpace(
                        cafeId, spaceId, request, authentication.getName()));
    }

    /**
     * Enables or disables new bookings for a study space.
     */
    @PatchMapping("/{spaceId}/availability")
    @Operation(summary = "Update study-space availability")
    public ResponseEntity<StudySpaceResponse> updateAvailability(
            @PathVariable Long cafeId,
            @PathVariable Long spaceId,
            @Valid @RequestBody
            UpdateStudySpaceAvailabilityRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                studySpaceService.updateAvailability(
                        cafeId,
                        spaceId,
                        request,
                        authentication.getName()
                )
        );
    }
}
