package com.ga.career_service.service;

import com.ga.career_service.dto.*;

import com.ga.career_service.exception.*;
import com.ga.career_service.model.*;
import com.ga.career_service.repository.*;

import com.ga.career_service.security.CurrentUser;
import com.ga.career_service.security.JwtPrincipal;
import com.ga.career_service.util.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides business logic for creating, retrieving, updating,
 * closing, filtering, and paginating job postings.
 */
@Service
public class JobService {

    private final JobRepository jobRepository;
    private final CurrentUser currentUser;
    private final IdentityClient identityClient;
    private final AuditService auditService;


    public JobService(
            JobRepository jobRepository,
            CurrentUser currentUser,
            IdentityClient identityClient,
            AuditService auditService
    ) {
        this.jobRepository = jobRepository;
        this.currentUser = currentUser;
        this.identityClient = identityClient;
        this.auditService = auditService;
    }


    public JobResponse createJob(
            String username,
            JobRequest request
    ) {
        JwtPrincipal user = getUser(username);

        // Only users with the approved EMPLOYER role can create jobs.
        if (user.role() != Role.EMPLOYER) {
            throw new ForbiddenException(
                    "Only approved employers can create jobs"
            );
        }

        Job job = new Job();

        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setLocation(request.getLocation());
        job.setJobType(request.getJobType());
        job.setSalary(request.getSalary());
        job.setDeadline(request.getDeadline());
        job.setVacancies(request.getVacancies() == null ? 1 : request.getVacancies());
        job.setFilledVacancies(0);



        // Preserve the existing project behavior for company names.
        job.setCompanyName(request.getCompanyName());

        job.setStatus(JobStatus.OPEN);
        job.setCreatedByUserId(user.id());

        Job savedJob = jobRepository.save(job);

        auditService.log(
                username,
                "JOB_CREATED",
                "JOB",
                savedJob.getId()
        );

        return toResponse(savedJob);
    }


    public List<JobResponse> getAllJobs() {
        return toResponses(jobRepository.findAll());
    }


    public PageResponse<JobResponse> getJobs(
            JobType jobType,
            JobStatus status,
            String location,
            String keyword,
            int page,
            int size,
            String sort,
            String direction
    ) {
        // Build the filters dynamically; null or blank values are ignored.

        Specification<Job> specification =
                Specs.<Job>equal(jobType, "jobType")
                        .and(Specs.equal(status, "status"))
                        .and(Specs.containsIgnoreCase(location, "location"))
                        .and(
                                Specs.<Job>containsIgnoreCase(keyword, "title")
                                        .or(
                                                Specs.containsIgnoreCase(
                                                        keyword, "companyName"
                                                )
                                        )
                        );


        // Newest jobs appear first by default.
        String effectiveDirection =
                (sort == null || sort.isBlank())
                        && (direction == null || direction.isBlank())
                        ? "desc"
                        : direction;

        // Only explicitly approved fields can be used for sorting.
        Pageable pageable = PagingUtil.createPageable(
                page,
                size,
                sort,
                effectiveDirection,
                "createdAt",
                Set.of("createdAt", "deadline", "salary", "title")
        );

        // Let the database filter and paginate instead of loading every job.
        Page<Job> result = jobRepository.findAll(
                specification,
                pageable
        );

        List<JobResponse> content = toResponses(result.getContent());

        return new PageResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }

    /**
     * Retrieves every job posted by the given employer, newest first
     * (open and closed), for the employer's own dashboard.
     */
    public List<JobResponse> getMyJobs(String username) {
        return toResponses(jobRepository
                .findByCreatedByUserIdOrderByCreatedAtDesc(getUser(username).id()));
    }


    public JobResponse getJobById(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Job not found")
                );

        return toResponse(job);
    }

    /**
     * Updates a job owned by the specified employer.
     *
     * @param id ID of the job to update
     * @param username username of the authenticated employer
     * @param request updated job details
     * @return the updated job as a response DTO
     */
    public JobResponse updateJob(
            Long id,
            String username,
            JobRequest request
    ) {
        JwtPrincipal user = getUser(username);

        if (user.role() != Role.EMPLOYER) {
            throw new ForbiddenException(
                    "Only approved employers can update jobs"
            );
        }

        Job job = jobRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Job not found")
                );

        // Employers may modify only their own jobs.
        checkOwnership(job, user);

        // Closed jobs cannot be edited.
        if (job.getStatus() == JobStatus.CLOSED) {
            throw new ForbiddenException(
                    "Closed jobs cannot be updated"
            );
        }

        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setCompanyName(request.getCompanyName());
        job.setLocation(request.getLocation());
        job.setJobType(request.getJobType());
        job.setSalary(request.getSalary());
        job.setDeadline(request.getDeadline());

        if (request.getVacancies() != null) {
            int filled = job.getFilledVacancies() == null ? 0 : job.getFilledVacancies();
            if (request.getVacancies() < filled) { throw new ConflictException( "Vacancies cannot be lower than the " + filled +" applicant(s) already accepted"
            );}
            job.setVacancies(request.getVacancies()); }

        Job savedJob = jobRepository.save(job);

        auditService.log(
                username,
                "JOB_UPDATED",
                "JOB",
                savedJob.getId()
        );

        return toResponse(savedJob);
    }


    public void deleteJob(Long id, String username) {
        JwtPrincipal user = getUser(username);

        if (user.role() != Role.EMPLOYER) {
            throw new ForbiddenException(
                    "Only approved employers can close jobs"
            );
        }

        Job job = jobRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Job not found")
                );

        checkOwnership(job, user);

        if (job.getStatus() == JobStatus.CLOSED) {
            throw new ForbiddenException("Job is already closed");
        }

        // Soft deletion preserves the job record and its history.
        job.setStatus(JobStatus.CLOSED);
        jobRepository.save(job);

        auditService.log(
                username,
                "JOB_CLOSED",
                "JOB",
                job.getId()
        );
    }

    /**
     * Returns the logged-in user from the JWT (id, username, role).
     * The username argument is the same authentication name the controller passes.
     */

    private JwtPrincipal getUser(String username) {
        return currentUser.get();
    }


    private void checkOwnership(Job job, JwtPrincipal user) {
        if (job.getCreatedByUserId() == null
                || !job.getCreatedByUserId().equals(user.id())) {
            throw new ForbiddenException(
                    "You are not allowed to modify this job"
            );
        }
    }

    private List<JobResponse> toResponses(List<Job> jobs) {
        Map<Long, String> names = identityClient.usernames(
                jobs.stream().map(Job::getCreatedByUserId).toList()
        );
        return jobs.stream().map(job -> toResponse(job, names)).toList();
    }

    private JobResponse toResponse(Job job) {
        return toResponse(job, identityClient.usernames(List.of(job.getCreatedByUserId())));
    }

    private JobResponse toResponse(Job job, Map<Long, String> names) {
        return new JobResponse(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                job.getCompanyName(),
                job.getLocation(),
                job.getJobType(),
                job.getSalary(),
                job.getDeadline(),
                job.getStatus(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                IdentityClient.displayName(names, job.getCreatedByUserId()),
                job.getVacancies() == null ? 1 : job.getVacancies(),
                job.getFilledVacancies() == null ? 0 : job.getFilledVacancies()

        );
    }
}
