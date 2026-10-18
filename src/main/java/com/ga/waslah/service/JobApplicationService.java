package com.ga.waslah.service;

import com.ga.waslah.dto.JobApplicationRequest;
import com.ga.waslah.dto.JobApplicationResponse;
import com.ga.waslah.exception.ConflictException;
import com.ga.waslah.exception.ForbiddenException;
import com.ga.waslah.exception.ResourceNotFoundException;
import com.ga.waslah.model.ApplicationStatus;
import com.ga.waslah.model.Job;
import com.ga.waslah.model.JobApplication;
import com.ga.waslah.model.JobStatus;
import com.ga.waslah.model.Role;
import com.ga.waslah.model.User;
import com.ga.waslah.repository.JobApplicationRepository;
import com.ga.waslah.repository.JobRepository;
import com.ga.waslah.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public JobApplicationService(
            JobApplicationRepository jobApplicationRepository,
            JobRepository jobRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
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

        User user = getUser(username);

        // Only normal users can apply
        if (user.getRole() != Role.USER) {
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
                user.getId()
        )) {
            throw new ConflictException(
                    "You have already applied for this job"
            );
        }

        JobApplication application = new JobApplication();

        application.setJob(job);
        application.setUser(user);
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

        User user = getUser(username);

        return jobApplicationRepository
                .findByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================
    // GET JOB APPLICATIONS
    // =========================

    public List<JobApplicationResponse> getJobApplications(
            Long jobId,
            String username
    ) {

        User user = getUser(username);

        if (user.getRole() != Role.EMPLOYER) {
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
        if (!job.getCreatedBy()
                .getUsername()
                .equals(username)) {

            throw new ForbiddenException(
                    "You are not allowed to view applications for this job"
            );
        }

        return jobApplicationRepository
                .findByJobId(jobId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================
    // UPDATE APPLICATION STATUS
    // =========================

    public JobApplicationResponse updateApplicationStatus(
            Long applicationId,
            ApplicationStatus newStatus,
            String username
    ) {

        User employer = getUser(username);

        if (employer.getRole() != Role.EMPLOYER) {
            throw new ForbiddenException(
                    "Only employers can update application status"
            );
        }

        JobApplication application =
                jobApplicationRepository.findById(applicationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Application not found"
                                ));

        Job job = application.getJob();

        // Only the owner of the job can update applications
        if (!job.getCreatedBy()
                .getUsername()
                .equals(username)) {

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

    private User getUser(String username) {

        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        ));
    }

    private JobApplicationResponse toResponse(
            JobApplication application
    ) {

        return new JobApplicationResponse(
                application.getId(),
                application.getJob().getId(),
                application.getJob().getTitle(),
                application.getUser().getUsername(),
                application.getStatus(),
                application.getCoverLetter(),
                application.getAppliedAt()
        );
    }
}