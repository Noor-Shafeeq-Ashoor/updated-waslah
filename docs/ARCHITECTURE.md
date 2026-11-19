# WASLAH 4.0 — Architecture

WASLAH runs as **four Spring Boot microservices** and a **React frontend served by Nginx**. In production they run as Docker containers on one EC2 host behind CloudFront, with Amazon RDS as the database and Kafka for events.

![AWS architecture](waslah-aws-architecture.png)

## 1. Why these four services

The monolith's packages were grouped by **business capability**, so each service can change and be deployed without the others:

| Service | Port | Business capability | Why separate |
| --- | ---: | --- | --- |
| Identity | 8081 | Who the user is and what they may do | Security-critical; every other service depends on its tokens |
| Career | 8082 | Jobs and applications | Independent domain with its own users (employers, job seekers) |
| Workspace | 8083 | Cafés, bookings, study sessions | The busiest and most concurrency-sensitive part; can be scaled alone (`--profile scale` runs a second instance) |
| Notification | 8084 | Telling users what happened | Must not slow down or break the actions that trigger it, so it receives events asynchronously |

## 2. Communication

| Type | Between | How | Used for |
| --- | --- | --- | --- |
| HTTP REST (synchronous) | Browser → services | Through Nginx; JWT in the `Authorization` header | All user actions |
| HTTP REST (synchronous) | Career / Workspace → Identity | Internal API `/api/internal/**`, `X-Internal-Token` header, 2-second timeouts | User details, role changes after an approval |
| Kafka (asynchronous) | Workspace → Notification | Topics `booking-events`, `session-events` | Notifications after booking and session changes |
| Server-Sent Events | Notification → Browser | `GET /api/notifications/stream` | Live notifications |

**Synchronous** calls are used when the caller needs the answer to continue (for example the applicant's name in a list). **Asynchronous** events are used when the caller does not need to wait: a booking is saved and answered at once, and the notification follows. If Notification is down, the booking still works and the events are delivered later.

**No API gateway service:** Nginx is the single entry point. It routes by path, keeps SSE connections open without buffering, and blocks the internal API from outside.

## 3. Authentication across services

1. The user logs in at Identity (`POST /auth/login`) and receives a signed JWT with the user id, username and role.
2. The browser sends it with every request.
3. Each service validates the signature with the shared secret and applies role and ownership checks itself. No service calls Identity just to check a token.

## 4. Data ownership

One PostgreSQL database, **18 tables, each owned by one service**. A service reads and writes only its own tables and refers to other services' data by id (mostly `user_id`). See [ERD.md](ERD.md).

This is a deliberate trade-off: one managed database keeps cost and operations low for the project, and clear ownership makes a later move to one database per service straightforward.

## 5. Reliability

- **Containers:** health checks on every container (`/actuator/health/readiness` for services), automatic restart, memory limits, and start-up order that waits for Kafka and healthy dependencies.
- **Kafka:** 3 retries, then a dead-letter topic; idempotent consumer (`processed_events`).
- **Concurrency:** row locks, a PostgreSQL advisory lock and unique constraints keep bookings, sessions and vacancies correct even with two Workspace instances.
- **Deployment:** the exact image version is started and must report healthy; any version can be redeployed for rollback.

## 6. Environments

| | Local | AWS |
| --- | --- | --- |
| Start | `docker compose ... compose.yaml up` | Jenkins → SSM → `deploy.sh` → `compose.prod.yaml` |
| Images | Built on the machine | Pulled from ECR, pinned version |
| Database | PostgreSQL container | Amazon RDS (private, TLS) |
| Configuration | `services/.env` | Parameter Store (encrypted) |
| Entry | http://localhost | CloudFront HTTPS URL |
