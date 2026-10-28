
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

    long countByStatus(com.ga.waslah.model.StudySessionStatus status);
    /**
    * Largest capacity among the active (OPEN/FULL) sessions held in a study
     * space, or null when there are none. Used to stop a manager from shrinking
     * a space below a session that is still running.
   */
    @Query("""
          SELECT MAX(s.capacity) FROM StudySession s
            WHERE s.booking.studySpace.id = :spaceId
              AND s.status IN :statuses
            """)
    Integer findMaxCapacityOfSessionsInSpace(
            @Param("spaceId") Long spaceId,
           @Param("statuses") java.util.Collection<com.ga.waslah.model.StudySessionStatus> statuses);


    Optional<StudySession> findByBooking_Id(Long bookingId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StudySession s WHERE s.id = :id")
    Optional<StudySession> findByIdForUpdate(@Param("id") Long id);

}
