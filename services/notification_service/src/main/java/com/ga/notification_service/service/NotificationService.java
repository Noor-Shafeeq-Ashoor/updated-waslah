
package com.ga.notification_service.service;

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

    private final ConcurrentHashMap<Long, Set<SseEmitter>> emitters =
            new ConcurrentHashMap<>();


    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(0L);

        emitters.computeIfAbsent(
                userId,
                key -> ConcurrentHashMap.newKeySet()
        ).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(error -> removeEmitter(userId, emitter));

        return emitter;
    }


    public void sendToUser(Long userId, String eventName, Object data) {
        Set<SseEmitter> userEmitters = emitters.get(userId);

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
                removeEmitter(userId, emitter);
                emitter.completeWithError(exception);
            }
        }
    }


    private void removeEmitter(Long userId, SseEmitter emitter) {
        Set<SseEmitter> userEmitters = emitters.get(userId);

        if (userEmitters != null) {
            userEmitters.remove(emitter);

            if (userEmitters.isEmpty()) {
                emitters.remove(userId, userEmitters);
            }
        }
    }
}
