package com.ga.waslah.controller;

import com.ga.waslah.dto.UserProfileRequest;
import com.ga.waslah.model.UserProfile;
import com.ga.waslah.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

@Tag(name = "Profile", description = "The authenticated user's own profile.")
@RestController
@RequestMapping("/api/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

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
    public ResponseEntity<UserProfile> getProfile(
            Authentication authentication
    ) {

        String username = authentication.getName();

        return ResponseEntity.ok(
                userProfileService.getProfile(username)
        );
    }

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
    public ResponseEntity<UserProfile> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UserProfileRequest request
    ) {

        String username = authentication.getName();

        return ResponseEntity.ok(
                userProfileService.updateProfile(username, request)
        );
    }

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

        String filename = image.getFilename();

        MediaType mediaType =
                filename != null && filename.endsWith(".png")
                        ? MediaType.IMAGE_PNG
                        : MediaType.IMAGE_JPEG;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(image);
    }

    @Operation(
            summary = "Upload my profile picture",
            description = "Multipart field 'image'. JPG or PNG, max 5 MB, validated by content. The file is stored on disk under a generated UUID name and only that name is saved in the database. The profile must already exist."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated with the new image"),
            @ApiResponse(responseCode = "400", description = "Missing, oversized, wrong-type or corrupt image"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    @PutMapping("/image")
    public ResponseEntity<UserProfile> updateProfileImage(
            @RequestParam("image") MultipartFile image,
            Authentication authentication
    ) {

        UserProfile profile = userProfileService.updateProfileImage(
                authentication.getName(),
                image
        );

        return ResponseEntity.ok(profile);
    }
}