
package com.ga.waslah.service;

import com.ga.waslah.model.Booking;
import com.ga.waslah.model.BookingStatus;
import com.ga.waslah.model.StudySession;
import com.ga.waslah.model.StudySessionStatus;
import com.ga.waslah.repository.BookingRepository;
import com.ga.waslah.repository.StudySessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Automatically completes expired bookings and their linked
 * study sessions.
 *
 * Uses the shared PostgreSQL advisory lock to prevent races
 * with booking cancellation and study-session operations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BookingCompletionScheduler {

    private final BookingRepository bookingRepository;
    private final StudySessionRepository studySessionRepository;
    private final ParticipationLockService participationLockService;

    /**
     * Runs every minute and completes confirmed bookings whose end time has passed.
     * Booking and session updates happen atomically
     * inside the same database transaction.
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void completeExpiredBookings() {

        // Acquire the same lock used by booking and
        // study-session writes, before reading the records.
        participationLockService.lockStudySessionWrites();

        LocalDateTime now = LocalDateTime.now();

        List<Booking> expiredBookings =
                bookingRepository.findByStatusAndEndTimeLessThan(
                        BookingStatus.CONFIRMED,
                        now
                );

        if (expiredBookings.isEmpty()) {
            return;
        }

        int completedSessions = 0;

        for (Booking booking : expiredBookings) {

            // The booking has expired and was still confirmed.
            booking.setStatus(BookingStatus.COMPLETED);

            // A booking may or may not have a study session.
            StudySession session = studySessionRepository
                    .findByBooking_Id(booking.getId())
                    .orElse(null);

            if (session != null &&
                    (session.getStatus() == StudySessionStatus.OPEN ||
                            session.getStatus() == StudySessionStatus.FULL)) {

                session.setStatus(StudySessionStatus.COMPLETED);
                completedSessions++;
            }
        }

        // Persist both types of changes in one transaction.
        // If the transaction fails, both are rolled back.
        bookingRepository.saveAll(expiredBookings);

        log.info(
                "Automatically completed {} bookings and {} study sessions",
                expiredBookings.size(),
                completedSessions
        );
    }
}
