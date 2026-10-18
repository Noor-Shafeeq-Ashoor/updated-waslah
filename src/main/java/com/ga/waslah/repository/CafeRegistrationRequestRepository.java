package com.ga.waslah.repository;

import com.ga.waslah.model.CafeRegistrationRequest;
import com.ga.waslah.model.CafeRequestStatus;
import com.ga.waslah.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CafeRegistrationRequestRepository
        extends JpaRepository<CafeRegistrationRequest, Long> {

    // Get all requests submitted by a specific user.
    List<CafeRegistrationRequest> findByRequestedBy(User user);

    // Get all requests with a specific status.
    List<CafeRegistrationRequest> findByStatus(CafeRequestStatus status);
}