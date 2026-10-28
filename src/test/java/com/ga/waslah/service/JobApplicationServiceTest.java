package com.ga.waslah.service;

import com.ga.waslah.exception.ConflictException;
import com.ga.waslah.exception.ForbiddenException;
import com.ga.waslah.model.ApplicationStatus;
import com.ga.waslah.model.Job;
import com.ga.waslah.model.JobApplication;
import com.ga.waslah.model.JobStatus;
import com.ga.waslah.model.Role;
import com.ga.waslah.model.User;
import com.ga.waslah.repository.JobApplicationRepository;
import com.ga.waslah.repository.JobRepository;
import com.ga.waslah.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Vacancy rules when an employer accepts or rejects applicants. */
@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    @Mock private JobApplicationRepository jobApplicationRepository;
    @Mock private JobRepository jobRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @InjectMocks private JobApplicationService service;

    private User employer;
    private Job job;
    private JobApplication application;

    @BeforeEach
    void setUp() {
        employer = new User();
        employer.setId(1L);
        employer.setUsername("employer1");
        employer.setRole(Role.EMPLOYER);

        User applicant = new User();
        applicant.setId(2L);
        applicant.setUsername("student1");
        applicant.setRole(Role.USER);

        job = new Job();
        job.setId(10L);
        job.setTitle("Backend Developer");
        job.setCreatedBy(employer);
        job.setStatus(JobStatus.OPEN);
        job.setDeadline(LocalDateTime.now().plusDays(10));
        job.setVacancies(2);
        job.setFilledVacancies(0);

        application = new JobApplication();
        application.setId(100L);
        application.setJob(job);
        application.setUser(applicant);
        application.setStatus(ApplicationStatus.PENDING);
        application.setAppliedAt(LocalDateTime.now());

        lenient().when(userRepository.findByUsername("employer1")).thenReturn(Optional.of(employer));
        lenient().when(jobApplicationRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(application));
        lenient().when(jobRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(job));
        lenient().when(jobApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void acceptingCountsOneVacancyAndKeepsJobOpenWhileSeatsRemain() {
        service.updateApplicationStatus(100L, ApplicationStatus.ACCEPTED, "employer1");

        assertEquals(1, job.getFilledVacancies());
        assertEquals(JobStatus.OPEN, job.getStatus());
        assertEquals(ApplicationStatus.ACCEPTED, application.getStatus());
    }

    @Test
    void acceptingTheLastVacancyClosesTheJob() {
        job.setFilledVacancies(1);

        service.updateApplicationStatus(100L, ApplicationStatus.ACCEPTED, "employer1");

        assertEquals(2, job.getFilledVacancies());
        assertEquals(JobStatus.CLOSED, job.getStatus());
    }

    @Test
    void acceptingWhenAllVacanciesAreFilledIsRejectedAndNothingIsSaved() {
        job.setFilledVacancies(2);

        assertThrows(ConflictException.class,
                () -> service.updateApplicationStatus(100L, ApplicationStatus.ACCEPTED, "employer1"));

        assertEquals(2, job.getFilledVacancies());
        assertEquals(ApplicationStatus.PENDING, application.getStatus());
        verify(jobApplicationRepository, never()).save(any());
    }

    @Test
    void rejectingDoesNotUseUpAVacancy() {
        service.updateApplicationStatus(100L, ApplicationStatus.REJECTED, "employer1");

        assertEquals(0, job.getFilledVacancies());
        assertEquals(JobStatus.OPEN, job.getStatus());
        assertEquals(ApplicationStatus.REJECTED, application.getStatus());
    }

    @Test
    void anotherEmployerCannotDecideTheApplication() {
        User other = new User();
        other.setUsername("employer2");
        other.setRole(Role.EMPLOYER);
        when(userRepository.findByUsername("employer2")).thenReturn(Optional.of(other));

        assertThrows(ForbiddenException.class,
                () -> service.updateApplicationStatus(100L, ApplicationStatus.ACCEPTED, "employer2"));

        assertEquals(0, job.getFilledVacancies());
    }

    @Test
    void anApplicationThatWasAlreadyDecidedCannotBeAcceptedAgain() {
        application.setStatus(ApplicationStatus.ACCEPTED);

        assertThrows(ConflictException.class,
                () -> service.updateApplicationStatus(100L, ApplicationStatus.ACCEPTED, "employer1"));

        assertEquals(0, job.getFilledVacancies());
    }
}