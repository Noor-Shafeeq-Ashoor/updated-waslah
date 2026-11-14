package com.ga.career_service.service;

import com.ga.career_service.dto.JobApplicationRequest;
import com.ga.career_service.dto.JobApplicationResponse;
import com.ga.career_service.exception.ConflictException;
import com.ga.career_service.exception.ForbiddenException;
import com.ga.career_service.exception.ResourceNotFoundException;
import com.ga.career_service.model.ApplicationStatus;
import com.ga.career_service.model.Job;
import com.ga.career_service.model.JobApplication;
import com.ga.career_service.model.JobStatus;
import com.ga.career_service.model.Role;
import com.ga.career_service.repository.JobApplicationRepository;
import com.ga.career_service.repository.JobRepository;
import com.ga.career_service.security.CurrentUser;
import com.ga.career_service.security.JwtPrincipal;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobRepository jobRepository;
    private final CurrentUser currentUser;
    private final IdentityClient identityClient;
    private final AuditService auditService;

    public JobApplicationService(
            JobApplicationRepository jobApplicationRepository,
            JobRepository jobRepository,
            CurrentUser currentUser,
            IdentityClient identityClient,
            AuditService auditService
    ) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.jobRepository = jobRepository;
        this.currentUser = currentUser;
        this.identityClient = identityClient;
        this.auditService = auditService;
    }

    // =========================
    // APPLY FOR JOB
    // =========================

    public JobApplicationResponse applyForJob(
            Long jobId,
            String username,
            JobApplicationRequest request
    ) {

        JwtPrincipal user = getUser(username);

        // Only normal users can apply
        if (user.role() != Role.USER) {
            throw new ForbiddenException(
                    "Only users can apply for jobs"
            );
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job not found"
                        ));

        // Job must be open
        if (job.getStatus() != JobStatus.OPEN) {
            throw new ForbiddenException(
                    "You cannot apply for a closed job"
            );
        }

        // Deadline must not have passed
        if (job.getDeadline().isBefore(LocalDateTime.now())) {
            throw new ForbiddenException(
                    "The application deadline has passed"
            );
        }

        // Prevent duplicate application
        if (jobApplicationRepository.existsByJobIdAndUserId(
                jobId,
                user.id()
        )) {
            throw new ConflictException(
                    "You have already applied for this job"
            );
        }

        JobApplication application = new JobApplication();

        application.setJob(job);
        application.setUserId(user.id());
        application.setCoverLetter(request.getCoverLetter());
        application.setStatus(ApplicationStatus.PENDING);

        JobApplication savedApplication =
                jobApplicationRepository.save(application);

        auditService.log(
                username,
                "JOB_APPLICATION_SUBMITTED",
                "JOB_APPLICATION",
                savedApplication.getId()
        );

        return toResponse(savedApplication);
    }

    // =========================
    // GET MY APPLICATIONS
    // =========================

    public List<JobApplicationResponse> getMyApplications(
            String username
    ) {

        JwtPrincipal user = getUser(username);

        return toResponses(jobApplicationRepository.findByUserId(user.id()));
    }

    // =========================
    // GET JOB APPLICATIONS
    // =========================

    public List<JobApplicationResponse> getJobApplications(
            Long jobId,
            String username
    ) {

        JwtPrincipal user = getUser(username);

        if (user.role() != Role.EMPLOYER) {
            throw new ForbiddenException(
                    "Only employers can view job applications"
            );
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job not found"
                        ));

        // Employer can only see applications
        // for their own jobs
        if (!job.getCreatedByUserId().equals(user.id())) {

            throw new ForbiddenException(
                    "You are not allowed to view applications for this job"
            );
        }

        return toResponses(jobApplicationRepository.findByJobId(jobId));
    }

    // =========================
    // UPDATE APPLICATION STATUS
    // =========================

    @org.springframework.transaction.annotation.Transactional
    public JobApplicationResponse updateApplicationStatus(
            Long applicationId,
            ApplicationStatus newStatus,
            String username
    ) {

        JwtPrincipal employer = getUser(username);

        if (employer.role() != Role.EMPLOYER) {
            throw new ForbiddenException(
                    "Only employers can update application status"
            );
        }

        JobApplication application =
                jobApplicationRepository.findByIdForUpdate(applicationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Application not found"
                                ));

        Job job = jobRepository.findByIdForUpdate(application.getJob().getId()) .orElseThrow(() ->
                new ResourceNotFoundException("Job not found"));

        // Only the owner of the job can update applications
        if (!job.getCreatedByUserId().equals(employer.id())) {

            throw new ForbiddenException(
                    "You are not allowed to update this application"
            );
        }

        // Prevent changing final states
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new ConflictException(
                    "Application status can only be changed while pending"
            );
        }

        if (newStatus != ApplicationStatus.ACCEPTED &&
                newStatus != ApplicationStatus.REJECTED) {

            throw new ConflictException(
                    "Application can only be accepted or rejected"
            );
        }
        if (newStatus == ApplicationStatus.ACCEPTED) {
            int vacancies = job.getVacancies() == null ? 1 : job.getVacancies();
            int filled = job.getFilledVacancies() == null ? 0 : job.getFilledVacancies();

            if (filled >= vacancies) {throw new ConflictException( "All vacancies for this job are already filled");}
            job.setFilledVacancies(filled + 1);
            // The last vacancy closes the job automatically.
            if (filled + 1 >= vacancies) {
                job.setStatus(JobStatus.CLOSED);}

            jobRepository.save(job);
        }

        application.setStatus(newStatus);

        JobApplication savedApplication =
                jobApplicationRepository.save(application);

        auditService.log(
                username,
                "JOB_APPLICATION_" + newStatus.name(),
                "JOB_APPLICATION",
                savedApplication.getId()
        );

        return toResponse(savedApplication);
    }

    // =========================
    // HELPER
    // =========================

    /** Logged-in user from the JWT; username is the same authentication name the controller passes. */
    private JwtPrincipal getUser(String username) {
        return currentUser.get();
    }

    private List<JobApplicationResponse> toResponses(List<JobApplication> applications) {
        Map<Long, String> names = identityClient.usernames(
                applications.stream().map(JobApplication::getUserId).toList()
        );
        return applications.stream().map(a -> toResponse(a, names)).toList();
    }

    private JobApplicationResponse toResponse(JobApplication application) {
        return toResponse(application, identityClient.usernames(List.of(application.getUserId())));
    }

    private JobApplicationResponse toResponse(
            JobApplication application,
            Map<Long, String> names
    ) {

        return new JobApplicationResponse(
                application.getId(),
                application.getJob().getId(),
                application.getJob().getTitle(),
                IdentityClient.displayName(names, application.getUserId()),
                application.getStatus(),
                application.getCoverLetter(),
                application.getAppliedAt()
        );
    }
}