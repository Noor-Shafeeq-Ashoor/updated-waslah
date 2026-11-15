
package com.ga.workspace_service.repository;

import com.ga.workspace_service.model.SessionParticipant;
import com.ga.workspace_service.model.StudySessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SessionParticipantRepository
        extends JpaRepository<SessionParticipant, Long> {

    long countBySession_Id(Long sessionId);

    boolean existsBySession_IdAndUserId(
            Long sessionId,
            Long userId
    );
    @Query("""
    SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
    FROM SessionParticipant p
    JOIN p.session s
    JOIN s.booking b
    WHERE p.userId = :userId
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

    Optional<SessionParticipant> findBySession_IdAndUserId(
            Long sessionId,
            Long userId
    );
}
