
package com.ga.waslah.repository;

import com.ga.waslah.model.SessionWaitlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessionWaitlistRepository
        extends JpaRepository<SessionWaitlist, Long> {

    boolean existsBySession_IdAndUser_Id(
            Long sessionId,
            Long userId
    );

    Optional<SessionWaitlist>
    findFirstBySession_IdOrderByPositionAsc(Long sessionId);

    Optional<SessionWaitlist>
    findFirstBySession_IdOrderByPositionDesc(Long sessionId);
}
