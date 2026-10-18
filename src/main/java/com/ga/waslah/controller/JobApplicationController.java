package com.ga.waslah.controller;

import com.ga.waslah.dto.JobApplicationRequest;
import com.ga.waslah.dto.JobApplicationResponse;
import com.ga.waslah.model.ApplicationStatus;
import com.ga.waslah.service.JobApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Job Applications", description = "Applying to jobs and deciding on applications.")
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    // =========================
    // APPLY FOR JOB
    // =========================

    @Operation(
            summary = "Apply to a job",
            description = "USER role only. The job must be OPEN and before its deadline; one application per user per job."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Application submitted"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid request"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated but not allowed (wrong role or not the owner)"),
            @ApiResponse(responseCode = "404", description = "Resource not found"),
            @ApiResponse(responseCode = "409", description = "Already applied to this job")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/jobs/{jobId}")
    public ResponseEntity<JobApplicationResponse> applyForJob(
            @PathVariable Long jobId,
            @Valid @RequestBody JobApplicationRequest request,
            Authentication authentication
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        jobApplicationService.applyForJob(
                                jobId,
                                authentication.getName(),
                                request
                        )
                );
    }

    // =========================
    // MY APPLICATIONS
    // =========================

    @Operation(
            summary = "List my applications",
            description = "Applications submitted by the caller."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of applications"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT")
    })
    @GetMapping("/my")
    public ResponseEntity<List<JobApplicationResponse>>
    getMyApplications(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                jobApplicationService.getMyApplications(
                        authentication.getName()
                )
        );
    }

    // =========================
    // JOB APPLICATIONS
    // =========================

    @Operation(
            summary = "List applications for my job",
            description = "EMPLOYER who owns the job only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of applications"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated but not allowed (wrong role or not the owner)"),
            @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<List<JobApplicationResponse>>
    getJobApplications(
            @PathVariable Long jobId,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                jobApplicationService.getJobApplications(
                        jobId,
                        authentication.getName()
                )
        );
    }

    // =========================
    // UPDATE STATUS
    // =========================

    @Operation(
            summary = "Accept or reject an application",
            description = "Query parameter status = ACCEPTED | REJECTED. Only PENDING applications can be decided; EMPLOYER who owns the job only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application updated"),
            @ApiResponse(responseCode = "400", description = "Invalid status value"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated but not allowed (wrong role or not the owner)"),
            @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    @PutMapping("/{applicationId}/status")
    public ResponseEntity<JobApplicationResponse>
    updateApplicationStatus(
            @PathVariable Long applicationId,
            @RequestParam ApplicationStatus status,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                jobApplicationService.updateApplicationStatus(
                        applicationId,
                        status,
                        authentication.getName()
                )
        );
    }
}