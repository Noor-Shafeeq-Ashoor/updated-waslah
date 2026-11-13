package com.ga.identity_service.repository;

import com.ga.identity_service.model.EmployerRequest;
import com.ga.identity_service.model.EmployerRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployerRequestRepository
        extends JpaRepository<EmployerRequest, Long> {

    Optional<EmployerRequest> findByUserUsername(String username);

    List<EmployerRequest> findByStatus(EmployerRequestStatus status);

    long countByStatus(EmployerRequestStatus status);
}