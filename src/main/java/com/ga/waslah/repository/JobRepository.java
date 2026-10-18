
package com.ga.waslah.repository;

import com.ga.waslah.model.Job;
import com.ga.waslah.model.JobStatus;
import com.ga.waslah.model.JobType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface JobRepository extends
        JpaRepository<Job, Long>,
        JpaSpecificationExecutor<Job> {

    /**
     * Finds jobs matching the specified status.
     *
     * @param status the status to search for
     * @return a list of jobs with the specified status
     */
    List<Job> findByStatus(JobStatus status);

    /**
     * Finds jobs matching the specified job type.
     *
     * @param jobType the type of job to search for
     * @return a list of jobs with the specified type
     */
    List<Job> findByJobType(JobType jobType);

    /**
     * Finds jobs in the specified location, ignoring case differences.
     *
     * @param location the location to search for
     * @return a list of jobs in the specified location
     */
    List<Job> findByLocationIgnoreCase(String location);

    /**
     * Finds jobs whose titles contain the specified text,
     * ignoring case differences.
     *
     * @param title the text to search for in job titles
     * @return a list of matching jobs
     */
    List<Job> findByTitleContainingIgnoreCase(String title);
}
