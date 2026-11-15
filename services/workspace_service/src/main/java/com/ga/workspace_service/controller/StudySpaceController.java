
package com.ga.workspace_service.controller;

import com.ga.workspace_service.dto.PageResponse;
import com.ga.workspace_service.dto.StudySpaceResponse;
import com.ga.workspace_service.service.StudySpaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
