package com.ga.waslah.service;

import com.ga.waslah.dto.JobRequest;
import com.ga.waslah.dto.JobResponse;
import com.ga.waslah.dto.PageResponse;
import com.ga.waslah.exception.ConflictException;
import com.ga.waslah.exception.ForbiddenException;
import com.ga.waslah.exception.ResourceNotFoundException;
import com.ga.waslah.model.Job;
import com.ga.waslah.model.JobStatus;
import com.ga.waslah.model.JobType;
import com.ga.waslah.model.Role;
import com.ga.waslah.model.User;
import com.ga.waslah.repository.JobRepository;
import com.ga.waslah.repository.UserRepository;
import com.ga.waslah.util.PagingUtil;
import com.ga.waslah.util.Specs;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Provides business logic for creating, retrieving, updating,
 * closing, filtering, and paginating job postings.
 */
@Service
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    /**
     * Creates the service with its required repositories.
     *
     * @param jobRepository repository used to access job records
     * @param userRepository repository used to access user records
     */
    public JobService(
            JobRepository jobRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /**
     * Creates a new job for an approved employer.
     *
     * @param username username of the authenticated employer
     * @param request job details supplied by the client
     * @return the created job as a response DTO
     * @throws ResourceNotFoundException if the user does not exist
     * @throws ForbiddenException if the user is not an employer
     */
    public JobResponse createJob(
            String username,
            JobRequest request
    ) {
        User user = getUser(username);

        // Only users with the approved EMPLOYER role can create jobs.
        if (user.getRole() != Role.EMPLOYER) {
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
        job.setCreatedBy(user);

        Job savedJob = jobRepository.save(job);

        auditService.log(
                username,
                "JOB_CREATED",
                "JOB",
                savedJob.getId()
        );

        return toResponse(savedJob);
    }

    /**
     * Retrieves all jobs without pagination.
     *
     * <p>This method is retained for backward compatibility with
     * existing callers. The jobs endpoint will use {@link #getJobs}
     * when its controller is updated.</p>
     *
     * @return all jobs as response DTOs
     */
    public List<JobResponse> getAllJobs() {
        return jobRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Retrieves jobs using optional filters, pagination, and sorting.

     * <p>Supported filters include job type, status, location, and a
     * keyword matching the job title or company name.</p>
   */
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

        List<JobResponse> content = result.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

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
        return jobRepository
                .findByCreatedBy_UsernameOrderByCreatedAtDesc(username)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Retrieves a job by its ID.
     *
     * @param id ID of the requested job
     * @return the job as a response DTO
     * @throws ResourceNotFoundException if the job does not exist
     */
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
        User user = getUser(username);

        if (user.getRole() != Role.EMPLOYER) {
            throw new ForbiddenException(
                    "Only approved employers can update jobs"
            );
        }

        Job job = jobRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Job not found")
                );

        // Employers may modify only their own jobs.
        checkOwnership(job, username);

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

    /**
     * Closes a job owned by the specified employer.
     *
     * <p>The job is not physically deleted; its status is changed to CLOSED.</p>
     *
     * @param id ID of the job to close
     * @param username username of the authenticated employer
     */
    public void deleteJob(Long id, String username) {
        User user = getUser(username);

        if (user.getRole() != Role.EMPLOYER) {
            throw new ForbiddenException(
                    "Only approved employers can close jobs"
            );
        }

        Job job = jobRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Job not found")
                );

        checkOwnership(job, username);

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
     * Retrieves a user by username.
     *
     * @param username username to search for
     * @return the matching user
     * @throws ResourceNotFoundException if the user does not exist
     */
    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );
    }

    /**
     * Verifies that a job belongs to the specified employer.
     *
     * @param job job whose ownership must be checked
     * @param username username of the requesting user
     * @throws ForbiddenException if the user does not own the job
     */
    private void checkOwnership(Job job, String username) {
        if (job.getCreatedBy() == null
                || !job.getCreatedBy().getUsername().equals(username)) {
            throw new ForbiddenException(
                    "You are not allowed to modify this job"
            );
        }
    }

    /**
     * Converts a job entity into its API response DTO.
     *
     * @param job job entity to convert
     * @return the corresponding job response
     */
    private JobResponse toResponse(Job job) {
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
                job.getCreatedBy().getUsername(),
                job.getVacancies() == null ? 1 : job.getVacancies(),
                job.getFilledVacancies() == null ? 0 : job.getFilledVacancies()

        );
    }
}
