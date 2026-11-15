package com.ga.workspace_service.service;

import java.util.Collection;

/**
 * The only way workspace-service sends notifications.
 * Same fields notification-service stores: type, message, reference type and id, plus recipients.
 * Implementations: LoggingNotificationPublisher now; a Kafka publisher replaces it in the Kafka step.
 */
public interface NotificationPublisher {

    void publish(
            String type,
            Collection<Long> recipientUserIds,
            String message,
            String referenceType,
            Long referenceId
    );
}
