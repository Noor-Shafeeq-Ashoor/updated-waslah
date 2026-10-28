package com.ga.waslah.service;

import com.ga.waslah.dto.AdminStatsResponse;
import com.ga.waslah.model.BookingStatus;
import com.ga.waslah.model.CafeRequestStatus;
import com.ga.waslah.model.EmployerRequestStatus;
import com.ga.waslah.model.JobStatus;
import com.ga.waslah.model.Role;
import com.ga.waslah.model.StudySessionStatus;
import com.ga.waslah.model.UserStatus;
import com.ga.waslah.repository.BookingRepository;
import com.ga.waslah.repository.CafeRegistrationRequestRepository;
import com.ga.waslah.repository.EmployerRequestRepository;
import com.ga.waslah.repository.JobRepository;
import com.ga.waslah.repository.StudySessionRepository;
import com.ga.waslah.repository.UserRepository;
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
    private final CafeRegistrationRequestRepository cafeRegistrationRequestRepository;
    private final JobRepository jobRepository;
    private final BookingRepository bookingRepository;
    private final StudySessionRepository studySessionRepository;

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {

        Map<String, Long> byRole = new LinkedHashMap<>();
        for (Role role : Role.values()) {
            byRole.put(role.name(), userRepository.countByRole(role));
        }

        return new AdminStatsResponse(
                userRepository.count(),
                userRepository.countByStatus(UserStatus.ACTIVE),
                userRepository.countByStatus(UserStatus.INACTIVE),
                byRole,
                employerRequestRepository.countByStatus(EmployerRequestStatus.PENDING),
                cafeRegistrationRequestRepository.countByStatus(CafeRequestStatus.PENDING),
                jobRepository.countByStatus(JobStatus.OPEN),
                bookingRepository.countByStatus(BookingStatus.PENDING),
                bookingRepository.countByStatus(BookingStatus.CONFIRMED),
                studySessionRepository.countByStatus(StudySessionStatus.OPEN)
                        + studySessionRepository.countByStatus(StudySessionStatus.FULL)
        );
    }
}