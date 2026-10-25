
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
