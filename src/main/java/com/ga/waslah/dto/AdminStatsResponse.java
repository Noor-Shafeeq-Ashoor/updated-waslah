package com.ga.waslah.dto;

import java.util.Map;

/** Numbers shown on the admin dashboard. */
public record AdminStatsResponse(
        long totalUsers,
        long activeUsers,
        long inactiveUsers,
        Map<String, Long> usersByRole,
        long pendingEmployerRequests,
        long pendingCafeRequests,
        long openJobs,
        long pendingBookings,
        long confirmedBookings,
        long activeStudySessions
) {
}