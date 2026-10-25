
package com.ga.waslah.service;

import com.ga.waslah.dto.*;
import com.ga.waslah.exception.ForbiddenException;
import com.ga.waslah.model.Cafe;
import com.ga.waslah.model.StudySpace;
import com.ga.waslah.repository.CafeRepository;
import com.ga.waslah.repository.StudySpaceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CafeManagerStudySpaceService {

    private final CafeRepository cafeRepository;
    private final StudySpaceRepository studySpaceRepository;

    /**
     * Creates a study space in a café managed by the authenticated user.
     *
     * @param cafeId café identifier
     * @param request validated study-space details
     * @param username authenticated manager's username
     * @return created study-space details
     */
    @Transactional
    public StudySpaceResponse createStudySpace(
            Long cafeId,
            CreateStudySpaceRequest request,
            String username
    ) {

        Cafe cafe = cafeRepository.findById(cafeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cafe not found"
                ));

        // Prevent managers from creating spaces in someone else's café.
        verifyOwnership(cafe, username);

        StudySpace space = new StudySpace();
        space.setName(request.name().trim());
        space.setCapacity(request.capacity());
        space.setPricePerHour(request.pricePerHour());
        space.setAvailable(true);
        space.setCafe(cafe);

        StudySpace saved = studySpaceRepository.save(space);

        log.info(
                "Study space created: id={}, cafeId={}",
                saved.getId(),
                cafeId
        );

        return toResponse(saved);
    }

    /**
     * Updates availability while holding a database write lock.
     *
     * Existing bookings remain unchanged. Booking creation must also
     * check availability while holding the same study-space lock.
     */
    @Transactional
    public StudySpaceResponse updateAvailability(
            Long cafeId,
            Long spaceId,
            UpdateStudySpaceAvailabilityRequest request,
            String username
    ) {

        StudySpace space = studySpaceRepository
                .findByIdForUpdate(spaceId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Study space not found"
                ));

        // Verify that the space belongs to the requested café.
        if (!space.getCafe().getId().equals(cafeId)) {
            throw new IllegalArgumentException(
                    "Study space does not belong to this cafe"
            );
        }

        // Verify manager ownership.
        verifyOwnership(space.getCafe(), username);

        space.setAvailable(request.available());

        log.info(
                "Study space availability updated: id={}, available={}",
                spaceId,
                request.available()
        );

        return toResponse(space);
    }

    /**
     * Ensures the authenticated manager owns the café.
     */
    private void verifyOwnership(Cafe cafe, String username) {

        if (!cafe.getManager().getUsername().equals(username)) {
            throw new ForbiddenException(
                    "You are not allowed to manage this cafe"
            );
        }
    }

    /**
     * Converts the entity into an API response DTO.
     */
    private StudySpaceResponse toResponse(StudySpace space) {

        return new StudySpaceResponse(
                space.getId(),
                space.getName(),
                space.getCapacity(),
                space.getPricePerHour(),
                space.getAvailable(),
                space.getCafe().getId()
        );
    }
}
