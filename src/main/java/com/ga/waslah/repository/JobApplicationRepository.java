
package com.ga.waslah.repository;

import com.ga.waslah.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface JobApplicationRepository extends
        JpaRepository<JobApplication, Long>,
        JpaSpecificationExecutor<JobApplication> {

    /**
     * Checks whether a user has already applied for a specific job.
     *
     * @param jobId the ID of the job
     * @param userId the ID of the user
     * @return true if an application already exists; otherwise false
     */
    boolean existsByJobIdAndUserId(Long jobId, Long userId);

    /**
     * Finds all job applications submitted by a specific user.
     *
     * @param userId the ID of the user
     * @return the user's job applications
     */
    List<JobApplication> findByUserId(Long userId);

    /**
     * Finds all applications submitted for a specific job.
     *
     * @param jobId the ID of the job
     * @return the applications for the specified job
     */
    List<JobApplication> findByJobId(Long jobId);
}
