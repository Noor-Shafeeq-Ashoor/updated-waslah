package com.ga.notification_service.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ga.notification_service.model.ProcessedEvent;
import com.ga.notification_service.repository.ProcessedEventRepository;
import com.ga.notification_service.service.StoredNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Parses an event and stores its notifications exactly once per eventId. */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventHandler {

    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final ProcessedEventRepository processedEventRepository;
    private final StoredNotificationService storedNotificationService;

    public NotificationEvent parse(String payload) {
        NotificationEvent event;
        try {
            event = JSON.readValue(payload, NotificationEvent.class);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new InvalidNotificationEventException("Event is not valid JSON", exception);
        }
        if (event == null || event.eventId() == null || event.eventId().isBlank()
                || event.eventType() == null || event.message() == null
                || event.recipientUserIds() == null || event.recipientUserIds().isEmpty()) {
            throw new InvalidNotificationEventException("Event is missing eventId, eventType, message or recipients");
        }
        return event;
    }

    /**
     * Stores the notifications and records the event id in one transaction.
     * Returns false if this event was already handled (a Kafka redelivery).
     * If two consumers race on the same event, the primary key on processed_events
     * makes one transaction fail; the retry then sees the event as handled.
     */
    @Transactional
    public boolean store(NotificationEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Event {} ({}) already handled, skipped", event.eventId(), event.eventType());
            return false;
        }

        int saved = storedNotificationService.saveForUsers(
                event.recipientUserIds(), event.eventType(), event.message(),
                event.referenceType(), event.referenceId());

        processedEventRepository.saveAndFlush(
                new ProcessedEvent(event.eventId(), event.eventType(), LocalDateTime.now()));

        log.info("Event {} ({}) stored for {} recipient(s)", event.eventId(), event.eventType(), saved);
        return true;
    }
}

