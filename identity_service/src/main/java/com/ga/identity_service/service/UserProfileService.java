package com.ga.identity_service.service;

import com.ga.identity_service.dto.UserProfileRequest;
import com.ga.identity_service.dto.UserProfileResponse;
import com.ga.identity_service.exception.ResourceNotFoundException;
import com.ga.identity_service.model.User;
import com.ga.identity_service.model.UserProfile;
import com.ga.identity_service.repository.UserRepository;
import com.ga.identity_service.repository.UserProfileRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

    @Transactional(readOnly = true)
    public UserProfile getProfile(String username) {

        User user = findUser(username);

        if (user.getUserProfile() == null) {
            throw new ResourceNotFoundException("Profile not found");
        }

        return user.getUserProfile();
    }

    @Transactional
    public UserProfile updateProfile(String username, UserProfileRequest request) {

        User user = findUser(username);
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

        return profile;
    }

    @Transactional
    public UserProfile updateProfileImage(String username, MultipartFile image) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserProfile profile = user.getUserProfile();

        if (profile == null) {
            throw new ResourceNotFoundException("Profile not found");
        }

        String oldFilename = profile.getProfileImage();
        String newFilename = profileImageStorageService.storeImage(image);

        try {
            profile.setProfileImage(newFilename);
            UserProfile saved = userProfileRepository.saveAndFlush(profile);

            // Do not delete the old file here: the database
            // transaction may still roll back.
            return saved;
        } catch (RuntimeException ex) {
            profileImageStorageService.deleteImage(newFilename);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Resource getProfileImage(String username) {

        UserProfile profile = getProfile(username);

        if (profile.getProfileImage() == null) {
            throw new ResourceNotFoundException("Profile image not found");
        }

        return profileImageStorageService.loadImage(profile.getProfileImage());
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

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));
    }


}