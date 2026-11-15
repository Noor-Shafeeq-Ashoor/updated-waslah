package com.ga.workspace_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;

/** Temporary publisher until Kafka is added: records each notification in the log only. */
@Component
@Slf4j
public class LoggingNotificationPublisher implements NotificationPublisher {

    @Override
    public void publish(String type, Collection<Long> recipientUserIds, String message,
                        String referenceType, Long referenceId) {
        log.info("Notification {} for users {} ({} {}): {}",
                type, recipientUserIds, referenceType, referenceId, message);
    }
}
