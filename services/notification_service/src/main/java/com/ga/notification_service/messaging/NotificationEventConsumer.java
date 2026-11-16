package com.ga.notification_service.messaging;

import com.ga.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationEventHandler handler;
    private final NotificationService notificationService;

    @KafkaListener(topics = {"${app.kafka.topics.booking-events}", "${app.kafka.topics.session-events}"})
    public void onMessage(ConsumerRecord<String, String> record) {
        log.info("Received event from {} partition={} offset={} key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        NotificationEvent event = handler.parse(record.value());

        if (handler.store(event)) {
            for (Long userId : event.recipientUserIds()) {
                notificationService.sendToUser(userId, event.eventType(), event.message());
            }
        }
    }
}