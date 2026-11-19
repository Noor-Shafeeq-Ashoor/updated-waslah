# WASLAH — Project Plan and Progress

WASLAH was built in three stages: a secure backend (Unit 2, Project 2), a concurrency extension for group study sessions (Unit 3, Project 3), and productionization into microservices deployed on AWS (Unit 4).

## Unit 4 — Productionization (Project 4)

**Goal:** move from "it works on my computer" to an application that is built, tested, containerized, deployed, monitored and maintained automatically.

| Day | Deliverable | Tasks | Status |
| --- | --- | --- | --- |
| 1 | Plan and service boundaries | Review requirements; split the monolith by business capability into Identity, Career, Workspace, Notification; decide JWT across services, Nginx as entry point, one shared database with table ownership; write the extraction plan | Completed |
| 2 | Identity service | Move users, profiles, tokens, employer requests, admin; add user id and role to the JWT; internal API protected by a token | Completed |
| 3 | Career service | Move jobs and applications; replace `User` links with user ids; read user details from Identity's internal API | Completed |
| 4 | Workspace service | Move cafés, study spaces, bookings, study sessions, waitlist; keep row locks and the advisory lock; scheduler for expiry and completion | Completed |
| 5 | Notification service + Kafka | Topics `booking-events` and `session-events`; producer in Workspace; consumer with retries, dead-letter topic and idempotency (`processed_events`); SSE delivery | Completed |
| 6 | Docker and Docker Compose | Dockerfile per service and frontend; Compose with PostgreSQL, Kafka, health checks, restart policies, memory limits, Kafka UI and a second Workspace instance | Completed |
| 7 | CI/CD | Jenkins pipeline as code: build, test, frontend build, versioned Docker images, push to ECR, release files to S3, manual approval, deploy, rollback; Jenkins configured as code | Completed |
| 8 | AWS deployment | ECR, S3, RDS PostgreSQL (private), EC2 with Docker, CloudFront HTTPS, Parameter Store secrets, IAM least privilege, CloudWatch alarms, budget; setup and teardown scripts | Completed |
| 9 | Testing | Context tests for all services; Cucumber booking scenarios; Testcontainers concurrency test on PostgreSQL 17 | Completed |
| 10 | Documentation and presentation | README, per-service ERD, architecture and AWS diagrams, Kafka and API docs, user stories, plan, demo | Completed |

### Requirement coverage

| Requirement | Where it is met |
| --- | --- |
| Microservices architecture | 4 Spring Boot services in `services/`, each with its own build, image and port |
| Docker | `Dockerfile` in each service and in `frontend/` |
| Docker Compose | `infrastructure/docker/compose.yaml` (local), `compose.prod.yaml` (AWS) |
| Kafka (asynchronous communication) | Workspace → `booking-events` / `session-events` → Notification; retries, DLT, idempotency |
| CI pipeline (build, tests, validation) | `Jenkinsfile` stages 1–3 on every branch; a failure stops the pipeline |
| CD pipeline (push images, deploy) | `Jenkinsfile` stages 4–7 on `master`, with approval and rollback |
| Image registry with meaningful tags | Amazon ECR, tags `1.0.<build>` and `sha-<commit>` |
| AWS deployment | EC2, RDS, ECR, S3, CloudFront, Systems Manager, IAM, CloudWatch |
| Cloud database | Amazon RDS for PostgreSQL 17, not publicly accessible |
| AWS security | Security groups, private database, secrets in Parameter Store, least-privilege IAM, no credentials in Git |
| Testing | Context tests, Cucumber BDD, Testcontainers integration test |
| Documentation | README, `docs/` (ERD, architecture, Kafka, API, CI/CD), plan, user stories |
| Caching | Not implemented; listed as the first next step (Spring Cache for job and café lists) |

### Main risks and how they were handled

| Risk | Handling |
| --- | --- |
| Breaking login and roles when splitting | JWT carries user id and role; role changes go through Identity's internal API |
| Lost or duplicated notifications | Kafka retries, dead-letter topic, `processed_events` table |
| Secrets leaking | `.env` ignored by Git; Parameter Store in AWS; Jenkins credentials store |
| Cost of AWS resources | Budget alert, small instance types, `teardown.sh` after the presentation |

---

## Unit 3 — Concurrency extension (Project 3)

| Day | Deliverable | Tasks | Status |
| --- | --- | --- | --- |
| 1 | Requirements and data model | Concurrency scenarios; design `StudySession`, `SessionParticipant`, `SessionWaitlist`; relationships and DTOs | Completed |
| 2 | Group study workflow | Sessions from confirmed bookings; organizer seat; join and leave; capacity and overlap rules | Completed |
| 3 | Concurrency protection | Transactions; PostgreSQL transaction-level advisory lock; booking and session changes coordinated | Completed |
| 4 | FIFO waitlist and integration | Queue when full; promote after leaving; booking cancellation cancels the session | Completed |
| 5 | Concurrent testing | Simultaneous join, overlapping join and concurrent leave tests; database checks | Completed |
| 6 | Documentation and API review | README, ERD, business rules, user stories; Swagger and logs | Completed |
| 7 | Final verification and presentation | Regression checks; demonstration evidence; presentation | Completed |

### Concurrency problem and solution

- **Shared resources:** session capacity, confirmed participants, waitlist order, linked booking and session state.
- **Risk:** two students taking the last seat at once could both be accepted; two departures could promote the wrong number of waiting students; joining could race with cancellation.
- **Solution:** every participation change takes the same PostgreSQL transaction-level advisory lock, so changes run one at a time across application instances; bookings also use row locks; unique constraints block duplicates.

### Verification evidence

| Scenario | Result | Status |
| --- | --- | --- |
| Two users join a session with one seat left | One `JOINED`, one `WAITLISTED` | Passed |
| Same user joins two overlapping sessions at once | One accepted, the other HTTP 409 | Passed |
| Two participants leave at once | Both HTTP 204; database state correct | Passed |
| FIFO waitlist promotion | Correct order in the database after departures | Passed |
| Booking cancellation at the same time as a join | Cancellation HTTP 200; join HTTP 409 | Passed (joiner had already joined; a fresh-user run is recommended) |
| Final booking and session state after cancellation | Both `CANCELLED` | Passed |

The tests called the running API against PostgreSQL using `ExecutorService`, `Callable`, `Future` and `CountDownLatch`. In Unit 4 the double-booking case is also covered automatically by `BookingConcurrencyIT` (Testcontainers).

---

## Unit 2 — Backend foundation (Project 2)

| Day | Deliverable | Tasks | Status |
| --- | --- | --- | --- |
| 1 | Planning and design | Requirements, scope, user stories, ERD, project setup | Completed |
| 2 | Database and foundation | PostgreSQL, entities, relationships, repositories, services | Completed |
| 3 | Core APIs | User, profile and job APIs | Completed |
| 4 | Authentication and authorization | Registration, login, JWT, roles | Completed |
| 5 | Account security | Password recovery and reset, rate limiting | Completed |
| 6 | Booking module | Booking APIs, validation and business rules | Completed |
| 7 | Testing and validation | API, authentication, authorization and validation testing | Completed |
