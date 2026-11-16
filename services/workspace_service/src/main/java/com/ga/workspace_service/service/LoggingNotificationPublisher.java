package com.ga.workspace_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collection;

/** Used when NOTIFICATION_PUBLISHER=log (tests, or running without Kafka): records each notification in the log only. */
@Component
@ConditionalOnProperty(name = "app.notifications.publisher", havingValue = "log")
@Slf4j
public class LoggingNotificationPublisher implements NotificationPublisher {

    @Override
    public void publish(String type, Collection<Long> recipientUserIds, String message,
                        String referenceType, Long referenceId) {
        log.info("Notification {} for users {} ({} {}): {}",
                type, recipientUserIds, referenceType, referenceId, message);
    }
}
