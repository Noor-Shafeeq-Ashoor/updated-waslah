
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


    List<Job> findByStatus(JobStatus status);
    long countByStatus(JobStatus status);

    /** Locks one job row so vacancy changes for the same job run one at a time. */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT j FROM Job j WHERE j.id = :id")
    java.util.Optional<Job> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);



    /** Jobs posted by one employer, newest first. */
    List<Job> findByCreatedBy_UsernameOrderByCreatedAtDesc(String username);

    List<Job> findByJobType(JobType jobType);


    List<Job> findByLocationIgnoreCase(String location);


    List<Job> findByTitleContainingIgnoreCase(String title);


}
