package com.ga.waslah.service;

import com.ga.waslah.dto.JobRequest;
import com.ga.waslah.exception.ForbiddenException;
import com.ga.waslah.model.Job;
import com.ga.waslah.model.JobStatus;
import com.ga.waslah.model.Role;
import com.ga.waslah.model.User;
import com.ga.waslah.repository.JobRepository;
import com.ga.waslah.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private JobService jobService;


    @Test
    void shouldRejectJobCreationForRegularUser() {

        // Arrange
        User user = new User();
        user.setUsername("noor");
        user.setRole(Role.USER);

        JobRequest request = new JobRequest();

        when(userRepository.findByUsername("noor"))
                .thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(
                ForbiddenException.class,
                () -> jobService.createJob("noor", request)
        );

        // Make sure no job was saved
        verify(jobRepository, never()).save(any());
    }

    @Test
    void shouldCloseOwnJobAndWriteAuditEntry() {

        User employer = new User();
        employer.setUsername("employer1");
        employer.setRole(Role.EMPLOYER);

        Job job = new Job();
        job.setId(5L);
        job.setCreatedBy(employer);
        job.setStatus(JobStatus.OPEN);

        when(userRepository.findByUsername("employer1"))
                .thenReturn(Optional.of(employer));
        when(jobRepository.findById(5L)).thenReturn(Optional.of(job));

        jobService.deleteJob(5L, "employer1");

        assertEquals(JobStatus.CLOSED, job.getStatus());
        verify(jobRepository).save(job);
        verify(auditService).log("employer1", "JOB_CLOSED", "JOB", 5L);
    }

    @Test
    void shouldNotCloseAnotherEmployersJob() {

        User employer = new User();
        employer.setUsername("employer1");
        employer.setRole(Role.EMPLOYER);

        User owner = new User();
        owner.setUsername("employer2");

        Job job = new Job();
        job.setId(5L);
        job.setCreatedBy(owner);
        job.setStatus(JobStatus.OPEN);

        when(userRepository.findByUsername("employer1"))
                .thenReturn(Optional.of(employer));
        when(jobRepository.findById(5L)).thenReturn(Optional.of(job));

        assertThrows(
                ForbiddenException.class,
                () -> jobService.deleteJob(5L, "employer1")
        );

        verify(jobRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any());
    }
}
