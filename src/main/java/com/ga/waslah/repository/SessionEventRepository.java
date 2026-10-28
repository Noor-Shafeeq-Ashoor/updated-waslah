package com.ga.waslah.repository;

import com.ga.waslah.model.SessionEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionEventRepository extends JpaRepository<SessionEvent, Long> {

    List<SessionEvent> findBySession_IdOrderByIdAsc(Long sessionId);
}
