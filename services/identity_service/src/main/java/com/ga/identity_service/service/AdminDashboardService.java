package com.ga.identity_service.service;

import com.ga.identity_service.dto.AdminStatsResponse;
import com.ga.identity_service.model.*;
import com.ga.identity_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/** Read-only counts for the admin dashboard. */
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final EmployerRequestRepository employerRequestRepository;
    private final InternalApiClient internalApiClient;

    public AdminStatsResponse getStats() {

        Map<String, Long> byRole = new LinkedHashMap<>();
        for (Role role : Role.values()) {
            byRole.put(role.name(), userRepository.countByRole(role));
        }

        Map<String, Long> career = internalApiClient.careerStats();
        Map<String, Long> workspace = internalApiClient.workspaceStats();

        return new AdminStatsResponse(
                userRepository.count(),
                userRepository.countByStatus(UserStatus.ACTIVE),
                userRepository.countByStatus(UserStatus.INACTIVE),
                byRole,
                employerRequestRepository.countByStatus(EmployerRequestStatus.PENDING),
                workspace.getOrDefault("pendingCafeRequests", 0L),
                career.getOrDefault("openJobs", 0L),
                workspace.getOrDefault("pendingBookings", 0L),
                workspace.getOrDefault("confirmedBookings", 0L),
                workspace.getOrDefault("activeStudySessions", 0L)
        );
    }
}