package com.ga.waslah.event;

import com.ga.waslah.service.NotificationService;
import com.ga.waslah.service.StoredNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Tests the CompletableFuture fan-out that delivers session notifications:
 * both tasks run, and a failure in one does not stop the other.
 */
@ExtendWith(MockitoExtension.class)
class SessionActivityListenerTest {

    @Mock
    private StoredNotificationService stored;

    @Mock
    private NotificationService sse;

    private final SessionActivityEvent event = new SessionActivityEvent(
            5L, "SESSION_PROMOTED", List.of("ali", "sara"), "A seat opened up");

    @Test
    void savesAndPushesToEveryRecipient() throws Exception {
        SessionActivityListener listener =
                new SessionActivityListener(stored, sse, Runnable::run);

        listener.dispatch(event).get(5, TimeUnit.SECONDS);

        verify(stored).saveForUsers(
                List.of("ali", "sara"), "SESSION_PROMOTED",
                "A seat opened up", "STUDY_SESSION", 5L);
        verify(sse).sendToUser("ali", "SESSION_PROMOTED", "A seat opened up");
        verify(sse).sendToUser("sara", "SESSION_PROMOTED", "A seat opened up");
    }

    @Test
    void aFailureWhileSavingDoesNotStopThePush() throws Exception {
        doThrow(new IllegalStateException("database down"))
                .when(stored).saveForUsers(anyCollection(), anyString(), anyString(), anyString(), anyLong());
        SessionActivityListener listener =
                new SessionActivityListener(stored, sse, Runnable::run);

        CompletableFuture<Void> done = listener.dispatch(event);

        assertDoesNotThrow(() -> done.get(5, TimeUnit.SECONDS));
        verify(sse).sendToUser("ali", "SESSION_PROMOTED", "A seat opened up");
    }

    @Test
    void aFailureWhilePushingDoesNotStopTheSave() throws Exception {
        doThrow(new IllegalStateException("socket closed"))
                .when(sse).sendToUser(anyString(), anyString(), any());
        SessionActivityListener listener =
                new SessionActivityListener(stored, sse, Runnable::run);

        CompletableFuture<Void> done = listener.dispatch(event);

        assertDoesNotThrow(() -> done.get(5, TimeUnit.SECONDS));
        verify(stored).saveForUsers(anyCollection(), anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void runsOnTheThreadPoolNotOnTheCallingThread() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("test-notification-" + thread.getId());
            return thread;
        });
        try {
            String callerThread = Thread.currentThread().getName();
            java.util.concurrent.atomic.AtomicReference<String> worker =
                    new java.util.concurrent.atomic.AtomicReference<>();
            org.mockito.Mockito.doAnswer(invocation -> {
                worker.set(Thread.currentThread().getName());
                return null;
            }).when(sse).sendToUser(anyString(), anyString(), any());

            new SessionActivityListener(stored, sse, pool)
                    .dispatch(event).get(5, TimeUnit.SECONDS);

            verify(sse, timeout(1000).atLeastOnce()).sendToUser(anyString(), anyString(), any());
            assertTrue(worker.get().startsWith("test-notification-"));
            assertTrue(!worker.get().equals(callerThread));
        } finally {
            pool.shutdownNow();
        }
    }
}
