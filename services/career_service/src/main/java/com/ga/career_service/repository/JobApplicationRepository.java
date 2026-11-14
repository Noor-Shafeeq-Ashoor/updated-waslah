
package com.ga.career_service.repository;

import com.ga.career_service.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface JobApplicationRepository extends
        JpaRepository<JobApplication, Long>,
        JpaSpecificationExecutor<JobApplication> {

    boolean existsByJobIdAndUserId(Long jobId, Long userId);

    List<JobApplication> findByUserId(Long userId);

    List<JobApplication> findByJobId(Long jobId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT a FROM JobApplication a WHERE a.id = :id")
    java.util.Optional<JobApplication> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

}
