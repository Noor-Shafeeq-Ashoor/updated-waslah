
package com.ga.waslah.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Server-Sent Events (SSE) connections and sends real-time
 * notifications to connected users.
 */
@Service
public class NotificationService {

    private final ConcurrentHashMap<String, Set<SseEmitter>> emitters =
            new ConcurrentHashMap<>();

    /**
     * Registers a new SSE connection for the specified user.
     *
     * @param username the username subscribing to notifications
     * @return the SSE emitter used to send events to the user
     */
    public SseEmitter subscribe(String username) {
        SseEmitter emitter = new SseEmitter(0L);

        emitters.computeIfAbsent(
                username,
                key -> ConcurrentHashMap.newKeySet()
        ).add(emitter);

        emitter.onCompletion(() -> removeEmitter(username, emitter));
        emitter.onTimeout(() -> removeEmitter(username, emitter));
        emitter.onError(error -> removeEmitter(username, emitter));

        return emitter;
    }

    /**
     * Sends a named SSE event to all active connections belonging
     * to the specified user.
     *
     * @param username the intended recipient's username
     * @param eventName the name of the event being sent
     * @param data the event payload
     */
    public void sendToUser(String username, String eventName, Object data) {
        Set<SseEmitter> userEmitters = emitters.get(username);

        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(
                        SseEmitter.event()
                                .name(eventName)
                                .data(data)
                );
            } catch (IOException | IllegalStateException exception) {
                removeEmitter(username, emitter);
                emitter.completeWithError(exception);
            }
        }
    }

    /**
     * Removes a completed, timed-out, or failed SSE connection.
     *
     * @param username the username associated with the connection
     * @param emitter the connection to remove
     */
    private void removeEmitter(String username, SseEmitter emitter) {
        Set<SseEmitter> userEmitters = emitters.get(username);

        if (userEmitters != null) {
            userEmitters.remove(emitter);

            if (userEmitters.isEmpty()) {
                emitters.remove(username, userEmitters);
            }
        }
    }
}
