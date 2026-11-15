package com.ga.workspace_service.controller;


import com.ga.workspace_service.model.BookingStatus;
import com.ga.workspace_service.model.CafeRequestStatus;
import com.ga.workspace_service.model.StudySessionStatus;
import com.ga.workspace_service.repository.BookingRepository;
import com.ga.workspace_service.repository.CafeRegistrationRequestRepository;
import com.ga.workspace_service.repository.StudySessionRepository;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Hidden
@RestController
@RequiredArgsConstructor
public class InternalStatsController {

    private final BookingRepository bookingRepository;
    private final StudySessionRepository studySessionRepository;
    private final CafeRegistrationRequestRepository cafeRegistrationRequestRepository;

    @GetMapping("/api/internal/stats")
    public Map<String, Long> stats() {
        return Map.of(
                "pendingBookings", bookingRepository.countByStatus(BookingStatus.PENDING),
                "confirmedBookings", bookingRepository.countByStatus(BookingStatus.CONFIRMED),
                "activeStudySessions", studySessionRepository.countByStatus(StudySessionStatus.OPEN)
                        + studySessionRepository.countByStatus(StudySessionStatus.FULL),
                "pendingCafeRequests", cafeRegistrationRequestRepository.countByStatus(CafeRequestStatus.PENDING)
        );
    }

}


