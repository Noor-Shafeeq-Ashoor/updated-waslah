package com.ga.waslah.service;

import com.ga.waslah.event.SessionActivityEvent;
import com.ga.waslah.model.SessionEvent;
import com.ga.waslah.model.SessionEventType;
import com.ga.waslah.model.StudySession;
import com.ga.waslah.model.User;
import com.ga.waslah.repository.SessionEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Single place where study-session changes are written to the session history
 * and announced to users.
 *
 * <p>Callers must already be inside the transaction (and holding the
 * participation lock) that made the change: the history row commits or rolls
 * back with it, and the notification is published only after it commits.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionActivityService {

    private final SessionEventRepository sessionEventRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Records a history entry and, when there are recipients, publishes a
     * notification event.
    */
    public void record(
            StudySession session,
            User user,
            SessionEventType type,
            String details,
            Collection<String> recipients,
            String message
    ) {
        SessionEvent entry = new SessionEvent();
        entry.setSession(session);
        entry.setUser(user);
        entry.setEventType(type);
        entry.setDetails(details);
        sessionEventRepository.save(entry);

        log.info("Session {} event {} (user={})", session.getId(), type,
                user == null ? "system" : user.getUsername());

        if (recipients == null || recipients.isEmpty()) {
            return;
        }

        List<String> distinct = recipients.stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                .stream().toList();

        if (!distinct.isEmpty()) {
            eventPublisher.publishEvent(new SessionActivityEvent(
                    session.getId(), "SESSION_" + type.name(), distinct, message));
        }
    }

    /** History only, no notification. */
    public void record(StudySession session, User user,
                       SessionEventType type, String details) {
        record(session, user, type, details, List.of(), null);
    }
}
