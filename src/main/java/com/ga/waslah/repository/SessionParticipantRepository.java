
package com.ga.waslah.repository;

import com.ga.waslah.model.SessionParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionParticipantRepository
        extends JpaRepository<SessionParticipant, Long> {

    long countBySession_Id(Long sessionId);

    boolean existsBySession_IdAndUser_Id(
            Long sessionId,
            Long userId
    );
}
