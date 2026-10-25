
package com.ga.waslah.dto;

public record UserProfileResponse(
        Long id,
        String firstName,
        String lastName,
        String phone,
        String bio,
        String location,
        String profileImage
) {
}
