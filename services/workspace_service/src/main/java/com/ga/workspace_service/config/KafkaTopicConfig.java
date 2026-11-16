package com.ga.workspace_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Topics this service publishes to; created on startup if missing. 3 partitions, keyed by booking/session id. */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic bookingEvents(@Value("${app.kafka.topics.booking-events}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic sessionEvents(@Value("${app.kafka.topics.session-events}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }
}
