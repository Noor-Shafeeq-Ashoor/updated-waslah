package com.ga.workspace_service.service;

import com.ga.workspace_service.messaging.NotificationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Publishes notifications to Kafka for notification-service.
 * Called after the database transaction commits (listeners) or after the change succeeded (controllers),
 * so a rolled-back change never produces an event. Never throws: a failed send is logged.
 * Limitation: if Kafka is unreachable when publishing, that event is lost (an outbox table would fix this).
 */
@Component
@ConditionalOnProperty(name = "app.notifications.publisher", havingValue = "kafka", matchIfMissing = true)
@Slf4j
public class KafkaNotificationPublisher implements NotificationPublisher {

    static final int SCHEMA_VERSION = 1;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String bookingTopic;
    private final String sessionTopic;

    public KafkaNotificationPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.kafka.topics.booking-events}") String bookingTopic,
            @Value("${app.kafka.topics.session-events}") String sessionTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.bookingTopic = bookingTopic;
        this.sessionTopic = sessionTopic;
    }

    @Override
    public void publish(String type, Collection<Long> recipientUserIds, String message,
                        String referenceType, Long referenceId) {

        List<Long> recipients = recipientUserIds == null ? List.of()
                : recipientUserIds.stream().filter(Objects::nonNull).distinct().toList();
        if (recipients.isEmpty()) {
            return;
        }

        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID().toString(), type, SCHEMA_VERSION, Instant.now().toString(),
                recipients, message, referenceType, referenceId);

        // Session events and booking events go to separate topics; the key keeps one booking's events in order.
        String topic = "STUDY_SESSION".equals(referenceType) ? sessionTopic : bookingTopic;
        String key = referenceId == null ? null : referenceId.toString();

        try {
            String payload = JSON.writeValueAsString(event);
            kafkaTemplate.send(topic, key, payload).whenComplete((result, error) -> {
                if (error != null) {
                    log.error("Kafka publish FAILED for {} event {} ({}); notification not delivered",
                            type, event.eventId(), error.getClass().getSimpleName());
                } else if (result != null) {
                    log.info("Published {} event {} to {} partition={} offset={}",
                            type, event.eventId(), topic,
                            result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                }
            });
        } catch (JsonProcessingException | RuntimeException exception) {
            log.error("Kafka publish FAILED for {} event {} ({}); notification not delivered",
                    type, event.eventId(), exception.getClass().getSimpleName());
        }
    }
}
