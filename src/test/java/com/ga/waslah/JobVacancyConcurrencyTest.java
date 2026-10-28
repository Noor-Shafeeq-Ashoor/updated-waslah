package com.ga.waslah;

import com.ga.waslah.exception.ConflictException;
import com.ga.waslah.model.ApplicationStatus;
import com.ga.waslah.model.Job;
import com.ga.waslah.model.JobApplication;
import com.ga.waslah.model.JobStatus;
import com.ga.waslah.model.JobType;
import com.ga.waslah.model.Role;
import com.ga.waslah.model.User;
import com.ga.waslah.model.UserStatus;
import com.ga.waslah.repository.JobApplicationRepository;
import com.ga.waslah.repository.JobRepository;
import com.ga.waslah.repository.UserRepository;
import com.ga.waslah.service.JobApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real threads against the real service and an in-memory database: six
 * applicants are accepted at the same moment for a job with two vacancies.
 * The job row lock must let exactly two succeed.
 */
@SpringBootTest
@ActiveProfiles("test")
class JobVacancyConcurrencyTest {

    @Autowired private JobApplicationService service;
    @Autowired private JobRepository jobRepository;
    @Autowired private JobApplicationRepository applicationRepository;
    @Autowired private UserRepository userRepository;

    private User newUser(String prefix, Role role) {
        User u = new User();
        String id = prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
        u.setUsername(id);
        u.setEmail(id + "@test.local");
        u.setPassword("x");
        u.setRole(role);
        u.setStatus(UserStatus.ACTIVE);
        u.setEmailVerified(true);
        return userRepository.save(u);
    }

    @Test
    void onlyAsManyApplicantsAsVacanciesCanBeAccepted() throws Exception {
        final int vacancies = 2;
        final int applicants = 6;

        User employer = newUser("employer", Role.EMPLOYER);
        Job job = new Job();
        job.setTitle("Race job");
        job.setDescription("Concurrency test");
        job.setCompanyName("Test Co");
        job.setLocation("Manama");
        job.setJobType(JobType.FULL_TIME);
        job.setDeadline(LocalDateTime.now().plusDays(5));
        job.setStatus(JobStatus.OPEN);
        job.setVacancies(vacancies);
        job.setFilledVacancies(0);
        job.setCreatedBy(employer);
        job = jobRepository.save(job);

        List<Long> applicationIds = new ArrayList<>();
        for (int i = 0; i < applicants; i++) {
            JobApplication a = new JobApplication();
            a.setJob(job);
            a.setUser(newUser("student", Role.USER));
            a.setStatus(ApplicationStatus.PENDING);
            applicationIds.add(applicationRepository.save(a).getId());
        }

        ExecutorService pool = Executors.newFixedThreadPool(applicants);
        CountDownLatch ready = new CountDownLatch(applicants);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try {
            for (Long id : applicationIds) {
                Callable<Boolean> task = () -> {
                    ready.countDown();
                    go.await(10, TimeUnit.SECONDS);
                    try {
                        service.updateApplicationStatus(id, ApplicationStatus.ACCEPTED, employer.getUsername());
                        return true;
                    } catch (ConflictException full) {
                        return false;
                    }
                };
                results.add(pool.submit(task));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS), "Workers did not become ready");
            go.countDown();

            int accepted = 0;
            for (Future<Boolean> f : results) {
                if (f.get(30, TimeUnit.SECONDS)) {
                    accepted++;
                }
            }

            Job reloaded = jobRepository.findById(job.getId()).orElseThrow();
            long acceptedInDb = applicationRepository.findByJobId(job.getId()).stream()
                    .filter(a -> a.getStatus() == ApplicationStatus.ACCEPTED).count();

            assertEquals(vacancies, accepted, "Exactly the number of vacancies may be accepted");
            assertEquals(vacancies, acceptedInDb);
            assertEquals(vacancies, reloaded.getFilledVacancies());
            assertEquals(JobStatus.CLOSED, reloaded.getStatus(), "A filled job closes itself");
        } finally {
            go.countDown();
            pool.shutdownNow();
        }
    }
}