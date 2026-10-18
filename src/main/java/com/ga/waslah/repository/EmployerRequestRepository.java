package com.ga.waslah.repository;

import com.ga.waslah.model.EmployerRequest;
import com.ga.waslah.model.EmployerRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployerRequestRepository
        extends JpaRepository<EmployerRequest, Long> {

    Optional<EmployerRequest> findByUserUsername(String username);

    List<EmployerRequest> findByStatus(EmployerRequestStatus status);
}