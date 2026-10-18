
package com.ga.waslah.controller;

import com.ga.waslah.dto.PageResponse;
import com.ga.waslah.dto.StudySpaceResponse;
import com.ga.waslah.service.StudySpaceService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for searching available study spaces.
 *
 * <p>Supports filtering, pagination, and sorting of study spaces.</p>
 */
@Tag(name = "Study Spaces")
@RestController
@RequestMapping("/api/study-spaces")
@RequiredArgsConstructor
public class StudySpaceController {

    private final StudySpaceService studySpaceService;

    /**
     * Retrieves available study spaces with optional filters and pagination.
     *
     * <p>Example:
     * GET /api/study-spaces?cafeId=1&minCapacity=4&maxPrice=5.000&page=0&size=10
     * </p>
     *
     * @param cafeId optional cafe ID filter
     * @param minCapacity optional minimum capacity
     * @param maxPrice optional maximum price per hour
     * @param page zero-based page number; defaults to 0
     * @param size number of results per page; defaults to 10
     * @param sort field used for sorting; defaults to name
     * @param direction sorting direction; defaults to ascending
     * @return a page of matching study spaces
     */
    @Operation(
            summary = "Search available study spaces (paginated)",
            description = "Only available spaces in active cafés. Filters: cafeId, minCapacity, maxPrice (per hour). Pagination: page, size (1-100). Sorting: sort = name | capacity | pricePerHour, direction = asc | desc."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of study spaces"),
            @ApiResponse(responseCode = "400", description = "Invalid page, size, sort field or direction"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT")
    })
    @GetMapping
    public ResponseEntity<PageResponse<StudySpaceResponse>>
    getAvailableStudySpaces(
            @RequestParam(required = false) Long cafeId,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        // Forward the request parameters to the service layer.
        return ResponseEntity.ok(
                studySpaceService.getStudySpaces(
                        cafeId,
                        minCapacity,
                        maxPrice,
                        page,
                        size,
                        sort,
                        direction
                )
        );
    }
}
