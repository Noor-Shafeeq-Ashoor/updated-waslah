package com.ga.waslah.service;

import com.ga.waslah.dto.UserProfileRequest;
import com.ga.waslah.exception.ResourceNotFoundException;
import com.ga.waslah.model.User;
import com.ga.waslah.model.UserProfile;
import com.ga.waslah.repository.UserProfileRepository;
import com.ga.waslah.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import com.ga.waslah.dto.UserProfileResponse;

@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ProfileImageStorageService profileImageStorageService;

    public UserProfileService(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            ProfileImageStorageService profileImageStorageService
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.profileImageStorageService = profileImageStorageService;
    }

    public UserProfile getProfile(String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        if (user.getUserProfile() == null) {
            throw new ResourceNotFoundException("Profile not found");
        }

        return user.getUserProfile();
    }

    public UserProfile updateProfile(
            String username,
            UserProfileRequest request
    ) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        UserProfile profile = user.getUserProfile();

        if (profile == null) {
            profile = new UserProfile();
            user.setUserProfile(profile);
        }

        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setPhone(request.getPhone());
        profile.setBio(request.getBio());
        profile.setLocation(request.getLocation());

        userRepository.save(user);

        return user.getUserProfile();
    }


    public UserProfile updateProfileImage(
            String username,
            MultipartFile image
    ) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        UserProfile profile = user.getUserProfile();

        if (profile == null) {
            throw new ResourceNotFoundException("Profile not found");
        }

        String previousFilename = profile.getProfileImage();

        // Store the image and receive its generated UUID filename.
        String storedFilename =
                profileImageStorageService.storeImage(image);

        // Save the generated filename, never the original filename.
        profile.setProfileImage(storedFilename);

        UserProfile savedProfile = userProfileRepository.save(profile);

        // Remove the replaced file so old pictures do not pile up on disk.
        profileImageStorageService.deleteImage(previousFilename);

        return savedProfile;
    }

    /**
     * Loads the caller's stored profile image.
     *
     * @param username username of the authenticated user
     * @return the image file as a resource
     * @throws ResourceNotFoundException if no image has been uploaded
     */
    public Resource getProfileImage(String username) {

        UserProfile profile = getProfile(username);

        if (profile.getProfileImage() == null) {
            throw new ResourceNotFoundException("Profile image not found");
        }

        return profileImageStorageService.loadImage(
                profile.getProfileImage()
        );
    }

    public UserProfileResponse toResponse(UserProfile profile) {

        return new UserProfileResponse(
                profile.getId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getPhone(),
                profile.getBio(),
                profile.getLocation(),
                profile.getProfileImage()
        );
    }


}