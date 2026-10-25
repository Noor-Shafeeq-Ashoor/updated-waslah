
package com.ga.waslah.repository;

import com.ga.waslah.model.StudySession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudySessionRepository
        extends JpaRepository<StudySession, Long> {
    boolean existsByBooking_Id(Long bookingId);
    Optional<StudySession> findByBooking_Id(Long bookingId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StudySession s WHERE s.id = :id")
    Optional<StudySession> findByIdForUpdate(@Param("id") Long id);
}
