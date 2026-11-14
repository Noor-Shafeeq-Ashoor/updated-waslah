package com.ga.career_service.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Hidden
@RestController
@RequiredArgsConstructor
public class InternalStatsController {

    private final JobRepository jobRepository;

    @GetMapping("/api/internal/stats")
    public Map<String, Long> stats() {
        return Map.of("openJobs", jobRepository.countByStatus(JobStatus.OPEN));
    }


}


