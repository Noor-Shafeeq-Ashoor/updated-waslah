package com.ga.workspace_service.event;


import com.ga.workspace_service.service.NotificationPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Delivers study-session notifications after the database transaction commits.
 *
 * <p>Two independent tasks run on the {@code notificationExecutor} pool:
 * saving the notification (database) and pushing it over SSE (network). They
 * are chained with {@link CompletableFuture}: a failure in one is logged and
 * does not stop the other, and nothing here can fail or slow down the request
 * that triggered the change.</p>

 * <p>The tasks never wait on each other or on the pool, so even a small pool
 * cannot deadlock.</p>
 */
@Component
@Slf4j
public class SessionActivityListener {

    private final NotificationPublisher notificationPublisher;
    private final Executor executor;

    public SessionActivityListener(
            NotificationPublisher notificationPublisher,
            @Qualifier("notificationExecutor") Executor executor
    ) {
        this.notificationPublisher = notificationPublisher;
        this.executor = executor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSessionActivity(SessionActivityEvent event) {
        try {
            dispatch(event);
        } catch (RuntimeException exception) {
            // For example the pool's queue is full. The change itself is already committed.
            log.error("Session notification {} for session {} could not be scheduled: {}",
                    event.type(), event.sessionId(), exception.toString());
        }
    }

    /**
     * Public so tests can wait for it. Completes normally even if publishing fails.
     */
    public CompletableFuture<Void> dispatch(SessionActivityEvent event) {
        return CompletableFuture
                .runAsync(() -> notificationPublisher.publish(
                        event.type(), event.recipientUserIds(), event.message(),
                        "STUDY_SESSION", event.sessionId()), executor)
                .exceptionally(error -> {
                    log.error("Publishing notification {} for session {} failed: {}",
                            event.type(), event.sessionId(), error.toString());
                    return null;
                });
    }
}
