# Kafka Event Documentation

Kafka events are **not HTTP endpoints**. They represent asynchronous messages published after application activities.

| Topic | Producer | Consumer | Purpose |
|---|---|---|---|
| `booking-events` | Workspace `KafkaNotificationPublisher` | Notification `NotificationEventConsumer` | Booking-related notifications |
| `session-events` | Workspace `KafkaNotificationPublisher` | Notification `NotificationEventConsumer` | Study-session notifications |

Source files inspected: `services/workspace_service/.../KafkaNotificationPublisher.java`, `KafkaTopicConfig.java`; `services/notification_service/.../NotificationEventConsumer.java` and both services' `application.yml`.

## Event handling
- Workspace selects `session-events` when the reference type is `STUDY_SESSION`, otherwise `booking-events`.
- Kafka records are JSON strings, with a key intended to preserve ordering for a booking-related aggregate.
- The consumer uses `@KafkaListener` for both topics and passes records to notification processing.
- Workspace `KafkaTopicConfig` declares **3 partitions, replication factor 1** for each topic.
- Topic names are configurable through `app.kafka.topics.booking-events` and `app.kafka.topics.session-events`.
- The project includes processed-event storage for consumer-side duplicate handling; do not assume exactly-once delivery across the entire workflow without end-to-end tests.

## Business event examples
Booking creation, confirmation, rejection and cancellation; study-session joining, waitlisting, promotion, leaving and cancellation. The exact serialized JSON schema must be maintained in the producer and consumer `NotificationEvent` classes together. A producer or consumer contract change should be tested before deployment.

## Reliability and limits
Kafka offers asynchronous delivery, but the local Compose file uses one broker with replication factor one. Test producer failures, retries, duplicate deliveries, consumer restarts and poison messages. Consider a transactional outbox and dead-letter topic for stronger production guarantees.
