# Kafka events

Workspace publishes an event after every booking or study-session change; Notification consumes it, stores a notification for each recipient and pushes it to their open SSE connection. The booking request does not wait for this.

| Topic | Partitions | Producer | Consumer | Event types |
| --- | ---: | --- | --- | --- |
| `booking-events` | 3 | Workspace (`KafkaNotificationPublisher`) | Notification (`NotificationEventConsumer`) | `BOOKING_CREATED`, `BOOKING_CONFIRMED`, `BOOKING_REJECTED`, `BOOKING_CANCELLED`, `BOOKING_CANCELLED_BY_MANAGER`, `BOOKING_EXPIRED` |
| `session-events` | 3 | Workspace | Notification | `SESSION_JOINED`, `SESSION_WAITLISTED`, `SESSION_PROMOTED`, `SESSION_LEFT`, `SESSION_LEFT_WAITLIST`, `SESSION_CANCELLED` |
| `booking-events.DLT`, `session-events.DLT` | 3 | Notification error handler | – (kept for inspection) | Events that failed 3 retries |

## Event format (JSON)

```json
{
  "eventId": "6f1c2a9e-0b7d-4c31-9a55-2f0e3d4b8c11",
  "eventType": "BOOKING_CONFIRMED",
  "schemaVersion": 1,
  "occurredAt": "2026-11-18T10:30:00",
  "recipientUserIds": [12],
  "message": "Your booking for Quiet Desk 2 was confirmed",
  "referenceType": "BOOKING",
  "referenceId": 42
}
```

- **Key:** the booking or session id, so all events about one booking land in the same partition and stay in order.
- **schemaVersion:** lets the format grow; the consumer ignores unknown fields.
- Topic names are configurable (`app.kafka.topics.*`). Setting `NOTIFICATION_PUBLISHER=log` replaces Kafka with logging (used by tests that run without a broker).

## Reliability

| Concern | Handling |
| --- | --- |
| Temporary failure (e.g. database busy) | Retried 3 times, 1 second apart (`DefaultErrorHandler` + `FixedBackOff`) |
| Event that keeps failing | Sent to `<topic>.DLT` so it does not block the partition |
| Same event delivered twice | `eventId` saved in `processed_events` in the same transaction as the notification; a repeated id is skipped |
| Notification service down | Events stay in Kafka and are consumed when it restarts |

**Limitation:** one Kafka broker (replication factor 1). Events survive container restarts but not the loss of the host; a production cluster would use several brokers, for example Amazon MSK.
