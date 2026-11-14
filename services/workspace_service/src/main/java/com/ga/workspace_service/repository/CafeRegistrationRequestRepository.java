package com.ga.workspace_service.repository;

import com.ga.workspace_service.model.CafeRegistrationRequest;
import com.ga.workspace_service.model.CafeRequestStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CafeRegistrationRequestRepository
        extends JpaRepository<CafeRegistrationRequest, Long> {

    // Get all requests submitted by a specific user.
    List<CafeRegistrationRequest> findByRequestedByUserId(Long userId);

    // Get all requests with a specific status.
    List<CafeRegistrationRequest> findByStatus(CafeRequestStatus status);

    long countByStatus(CafeRequestStatus status);
}