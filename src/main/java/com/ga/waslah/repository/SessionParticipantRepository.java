
package com.ga.waslah.repository;

import com.ga.waslah.model.SessionParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ga.waslah.model.StudySessionStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SessionParticipantRepository
        extends JpaRepository<SessionParticipant, Long> {

    long countBySession_Id(Long sessionId);

    boolean existsBySession_IdAndUser_Id(
            Long sessionId,
            Long userId
    );
    @Query("""
    SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
    FROM SessionParticipant p
    JOIN p.session s
    JOIN s.booking b
    WHERE p.user.id = :userId
      AND s.status IN :statuses
      AND b.startTime < :endTime
      AND b.endTime > :startTime
    """)
    boolean existsOverlappingSession(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("statuses") List<StudySessionStatus> statuses
    );
    List<SessionParticipant> findBySession_Id(Long sessionId);

    Optional<SessionParticipant> findBySession_IdAndUser_Id(
            Long sessionId,
            Long userId
    );
}
