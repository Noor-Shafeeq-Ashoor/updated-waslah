package com.ga.notification_service.config;

import com.ga.notification_service.messaging.InvalidNotificationEventException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;


import org.springframework.util.backoff.FixedBackOff;
/**
 * Failure handling for the notification consumer:
 * a failing event is retried 3 times, 1 second apart, then published to <topic>.DLT
 * (same partition) so it is kept for inspection and does not block later events.
 * Invalid events skip the retries. Spring Boot attaches this error handler to the listener.
 */
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<?, ?> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
        errorHandler.addNotRetryableExceptions(InvalidNotificationEventException.class);
        return errorHandler;
    }

    // Dead-letter topics need at least as many partitions as the source topics (3).
    @Bean
    public NewTopic bookingEventsDlt(@Value("${app.kafka.topics.booking-events}") String topic) {
        return TopicBuilder.name(topic + ".DLT").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic sessionEventsDlt(@Value("${app.kafka.topics.session-events}") String topic) {
        return TopicBuilder.name(topic + ".DLT").partitions(3).replicas(1).build();
    }
}
