
package com.ga.waslah.controller;

import com.ga.waslah.dto.UserProfileRequest;
import com.ga.waslah.dto.UserProfileResponse;
import com.ga.waslah.model.UserProfile;
import com.ga.waslah.service.UserProfileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for managing the authenticated user's profile.
 *
 * Supports:
 * - Retrieving profile details
 * - Creating or updating profile details
 * - Uploading a profile image
 * - Downloading the stored profile image
 *
 * Profile endpoints return response DTOs instead of JPA entities.
 * Image downloads continue to return the actual image resource.
 */
@Tag(
        name = "Profile",
        description = "The authenticated user's own profile."
)
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * Retrieves the authenticated user's profile.
     *
     * @param authentication current authenticated user
     * @return profile details as a response DTO
     */
    @Operation(
            summary = "Get my profile",
            description = "Returns the caller's profile (404 until it has been created with PUT /api/profile)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(
            Authentication authentication
    ) {

        String username = authentication.getName();

        UserProfile profile = userProfileService.getProfile(username);

        // Convert the entity into a safe API response.
        return ResponseEntity.ok(
                userProfileService.toResponse(profile)
        );
    }

    /**
     * Creates or updates the authenticated user's profile.
     *
     * @param authentication current authenticated user
     * @param request validated profile information
     * @return updated profile details as a response DTO
     */
    @Operation(
            summary = "Create or update my profile",
            description = "Creates the profile on first call. Phone must be exactly 8 digits."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile saved"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid request"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT")
    })
    @PutMapping
    public ResponseEntity<UserProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UserProfileRequest request
    ) {

        String username = authentication.getName();

        UserProfile updatedProfile = userProfileService.updateProfile(
                username,
                request
        );

        // Return the DTO rather than exposing the JPA entity.
        return ResponseEntity.ok(
                userProfileService.toResponse(updatedProfile)
        );
    }

    /**
     * Downloads the authenticated user's stored profile image.
     *
     * The response contains the actual image bytes, not JSON.
     *
     * @param authentication current authenticated user
     * @return image resource with the appropriate content type
     */
    @Operation(
            summary = "Download my profile picture",
            description = "Returns the caller's stored profile image (image/jpeg or image/png)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image file"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "404", description = "No profile or no image uploaded")
    })
    @GetMapping("/image")
    public ResponseEntity<Resource> getProfileImage(
            Authentication authentication
    ) {

        Resource image = userProfileService.getProfileImage(
                authentication.getName()
        );

        // Determine the image type from the stored file extension.
        String filename = image.getFilename();

        MediaType mediaType =
                filename != null
                        && filename.toLowerCase().endsWith(".png")
                        ? MediaType.IMAGE_PNG
                        : MediaType.IMAGE_JPEG;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(image);
    }

    /**
     * Uploads or replaces the authenticated user's profile image.
     *
     * The storage service validates the uploaded image and saves it
     * using a generated filename.
     *
     * @param image uploaded JPG or PNG image
     * @param authentication current authenticated user
     * @return updated profile details as a response DTO
     */
    @Operation(
            summary = "Upload my profile picture",
            description = "Multipart field 'image'. JPG or PNG, max 5 MB, validated by content. "
                    + "The file is stored on disk under a generated UUID name and only that "
                    + "name is saved in the database. The profile must already exist."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated with the new image"),
            @ApiResponse(responseCode = "400", description = "Missing, oversized, wrong-type or corrupt image"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    @PutMapping(
            value = "/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<UserProfileResponse> updateProfileImage(
            @RequestParam("image") MultipartFile image,
            Authentication authentication
    ) {

        UserProfile updatedProfile = userProfileService.updateProfileImage(
                authentication.getName(),
                image
        );

        // Return the updated filename as part of the profile DTO.
        return ResponseEntity.ok(
                userProfileService.toResponse(updatedProfile)
        );
    }
}
