package com.ga.waslah.event;

import com.ga.waslah.service.NotificationService;
import com.ga.waslah.service.StoredNotificationService;
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
 *
 * <p>The tasks never wait on each other or on the pool, so even a small pool
 * cannot deadlock.</p>
 */
@Component
@Slf4j
public class SessionActivityListener {

    private final StoredNotificationService storedNotificationService;
    private final NotificationService notificationService;
    private final Executor executor;

    public SessionActivityListener(
            StoredNotificationService storedNotificationService,
            NotificationService notificationService,
            @Qualifier("notificationExecutor") Executor executor
    ) {
        this.storedNotificationService = storedNotificationService;
        this.notificationService = notificationService;
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
     * Starts both delivery tasks and returns a future that completes when both
     * have finished (successfully or not). Public so tests can wait for it.
     */
    public CompletableFuture<Void> dispatch(SessionActivityEvent event) {
        long startedAt = System.nanoTime();
        log.info("Session notification {} started: session={}, recipients={}",
                event.type(), event.sessionId(), event.recipients().size());

        CompletableFuture<Void> persist = CompletableFuture
                .runAsync(() -> storedNotificationService.saveForUsers(
                        event.recipients(), event.type(), event.message(),
                        "STUDY_SESSION", event.sessionId()), executor)
                .exceptionally(error -> {
                    log.error("Saving notification {} for session {} failed: {}",
                            event.type(), event.sessionId(), error.toString());
                    return null;
                });

        CompletableFuture<Void> push = CompletableFuture
                .runAsync(() -> event.recipients().forEach(username ->
                        notificationService.sendToUser(
                                username, event.type(), event.message())), executor)
                .exceptionally(error -> {
                    log.error("Pushing notification {} for session {} failed: {}",
                            event.type(), event.sessionId(), error.toString());
                    return null;
                });

        return CompletableFuture.allOf(persist, push).whenComplete((ignored, error) ->
                log.info("Session notification {} finished: session={}, took {} ms",
                        event.type(), event.sessionId(),
                        (System.nanoTime() - startedAt) / 1_000_000));
    }
}
