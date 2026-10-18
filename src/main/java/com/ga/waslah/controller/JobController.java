package com.ga.waslah.controller;

import com.ga.waslah.dto.JobRequest;
import com.ga.waslah.dto.JobResponse;
import com.ga.waslah.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.ga.waslah.dto.PageResponse;
import com.ga.waslah.model.JobStatus;
import com.ga.waslah.model.JobType;

@Tag(name = "Jobs", description = "Job postings. Employers manage their own; every authenticated user can browse.")
@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    // =========================
    // CREATE
    // =========================

    @Operation(
            summary = "Create a job",
            description = "EMPLOYER only. The job starts OPEN and is owned by the caller."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Job created"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid request"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated but not allowed (wrong role or not the owner)")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<JobResponse> createJob(
            @Valid @RequestBody JobRequest request,
            Authentication authentication
    ) {

        JobResponse response = jobService.createJob(
                authentication.getName(),
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =========================
    // GET ALL
    // =========================

    @Operation(
            summary = "Search jobs (paginated)",
            description = "Optional filters: jobType, status (OPEN/CLOSED), location (contains), keyword (matches title or company name). Pagination: page (0-based), size (1-100, default 10). Sorting: sort = createdAt | deadline | salary | title, direction = asc | desc. Newest jobs first by default."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of jobs"),
            @ApiResponse(responseCode = "400", description = "Invalid page, size, sort field or direction"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT")
    })
    @GetMapping
    public ResponseEntity<PageResponse<JobResponse>> getAllJobs(
            @RequestParam(required = false) JobType jobType,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction
    ) {

        return ResponseEntity.ok(
                jobService.getJobs(
                        jobType,
                        status,
                        location,
                        keyword,
                        page,
                        size,
                        sort,
                        direction
                )
        );
    }

    // =========================
    // GET BY ID
    // =========================

    @Operation(
            summary = "Get a job",
            description = "Returns one job by id."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Job"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJobById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                jobService.getJobById(id)
        );
    }

    // =========================
    // UPDATE
    // =========================

    @Operation(
            summary = "Update my job",
            description = "EMPLOYER who owns the job only. A CLOSED job cannot be edited."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Job updated"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid request"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated but not allowed (wrong role or not the owner)"),
            @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<JobResponse> updateJob(
            @PathVariable Long id,
            @Valid @RequestBody JobRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                jobService.updateJob(
                        id,
                        authentication.getName(),
                        request
                )
        );
    }

    // =========================
    // CLOSE
    // =========================

    @Operation(
            summary = "Close my job (soft delete)",
            description = "Sets the job status to CLOSED; the record is kept. EMPLOYER who owns the job only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Job closed"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated but not allowed (wrong role or not the owner)"),
            @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<Void> deleteJob(
            @PathVariable Long id,
            Authentication authentication
    ) {

        jobService.deleteJob(
                id,
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }
}