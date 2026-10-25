
package com.ga.waslah.repository;

import com.ga.waslah.model.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudySessionRepository
        extends JpaRepository<StudySession, Long> {
    boolean existsByBooking_Id(Long bookingId);
    Optional<StudySession> findByBooking_Id(Long bookingId);
}
