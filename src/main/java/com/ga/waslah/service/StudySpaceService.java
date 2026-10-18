
package com.ga.waslah.service;

import com.ga.waslah.dto.PageResponse;
import com.ga.waslah.dto.StudySpaceResponse;
import com.ga.waslah.model.StudySpace;
import com.ga.waslah.repository.StudySpaceRepository;
import com.ga.waslah.util.PagingUtil;
import com.ga.waslah.util.Specs;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Handles business logic for retrieving and filtering study spaces.
 */
@Service
@RequiredArgsConstructor
public class StudySpaceService {

    private final StudySpaceRepository studySpaceRepository;

    /**
     * Retrieves all available study spaces belonging to active cafes.
     *
     * <p>This method is retained for compatibility with existing callers.</p>
     *
     * @return a list of available study spaces
     */
    public List<StudySpaceResponse> getAvailableStudySpaces() {
        return studySpaceRepository
                .findByAvailableTrueAndCafe_ActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Retrieves available study spaces using optional filters,
     * pagination, and sorting.
     *
     * @param cafeId optional ID of the cafe
     * @param minCapacity optional minimum capacity
     * @param maxPrice optional maximum hourly price
     * @param page zero-based page number
     * @param size number of results per page
     * @param sort field used to sort the results
     * @param direction sorting direction: asc or desc
     * @return a page of study spaces and pagination metadata
     */
    public PageResponse<StudySpaceResponse> getStudySpaces(
            Long cafeId,
            Integer minCapacity,
            java.math.BigDecimal maxPrice,
            int page,
            int size,
            String sort,
            String direction
    ) {
        // Return only available spaces belonging to active cafes.
        Specification<StudySpace> specification =
                Specs.<StudySpace>equal(Boolean.TRUE, "available")
                        .and(
                                Specs.<StudySpace>equal(
                                        Boolean.TRUE,
                                        "cafe.active"
                                )
                        )
                        .and(
                                Specs.<StudySpace>equal(
                                        cafeId,
                                        "cafe.id"
                                )
                        )
                        .and(
                                Specs.atLeast(
                                        minCapacity,
                                        "capacity"
                                )
                        )
                        .and(
                                Specs.atMost(
                                        maxPrice,
                                        "pricePerHour"
                                )
                        );

        // Restrict sorting to supported fields to avoid invalid sort requests.
        // The default sort is by study space name in ascending order.
        Pageable pageable = PagingUtil.createPageable(
                page,
                size,
                sort,
                direction,
                "name",
                Set.of("name", "capacity", "pricePerHour")
        );

        // Apply filters and pagination at database level.
        Page<StudySpace> studySpacePage =
                studySpaceRepository.findAll(
                        specification,
                        pageable
                );

        // Convert database entities into response DTOs.
        List<StudySpaceResponse> content =
                studySpacePage.getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        // Include page details so the client knows whether more results exist.
        return new PageResponse<>(
                content,
                studySpacePage.getNumber(),
                studySpacePage.getSize(),
                studySpacePage.getTotalElements(),
                studySpacePage.getTotalPages(),
                studySpacePage.isFirst(),
                studySpacePage.isLast()
        );
    }

    /**
     * Converts a study space entity into its response DTO.
     *
     * @param studySpace study space to convert
     * @return the study space response
     */
    private StudySpaceResponse toResponse(StudySpace studySpace) {
        return new StudySpaceResponse(
                studySpace.getId(),
                studySpace.getName(),
                studySpace.getCapacity(),
                studySpace.getPricePerHour(),
                studySpace.getCafe().getName(),
                studySpace.getCafe().getLocation()
        );
    }
}
