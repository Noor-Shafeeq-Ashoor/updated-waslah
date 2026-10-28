# WASLAH — Project Planning & Progress

WASLAH is a single evolving Spring Boot backend application for career opportunities and café study-space bookings. The initial development established authentication, user management, jobs, café management, bookings, and notifications. The next development iteration extended the same application with concurrency-safe group study sessions, participants, and FIFO waitlists.

## Development Plan — Backend Foundation (Project 2)

| Day | Main Deliverable | Tasks | Status |
|---|---|---|---|
| Day 1 | Planning & Design | Requirements review, project scope, user stories, ERD and project setup | Completed |
| Day 2 | Database & Backend Foundation | PostgreSQL setup, entities, relationships, repositories and service layer | Completed |
| Day 3 | Core APIs | User, profile and job APIs | Completed |
| Day 4 | Authentication & Authorization | Registration, login, JWT and role-based authorization | Completed |
| Day 5 | Account Security | Password recovery, password reset and rate limiting | Completed |
| Day 6 | Booking Module | Booking APIs, validation and booking business rules | Completed |
| Day 7 | Testing & Validation | API testing, authentication testing, authorization and validation testing | Implemented; final regression review pending |

## Development Plan — Concurrency Extension (Project 3)

| Day | Main Deliverable | Tasks | Status |
|---|---|---|---|
| Day 1 | Requirements & Data Model | Review concurrency scenarios; design StudySession, SessionParticipant, SessionWaitlist; update relationships and DTOs | Completed |
| Day 2 | Group Study Workflow | Create sessions from confirmed bookings; organizer membership; join/leave; capacity and overlap rules | Completed |
| Day 3 | Concurrency Protection | Transactional operations; PostgreSQL transaction-scoped advisory lock; coordinate booking and session changes | Completed |
| Day 4 | FIFO Waitlist & Integration | Queue participants when full; promote eligible users after leaving; link booking cancellation to session cancellation | Completed |
| Day 5 | Concurrent Testing | Execute simultaneous join, overlapping join and concurrent leave tests; inspect database results | Completed | 
| Day 6 | Documentation & API Review | Integrate README, ERD, business rules, user stories; review Swagger and logs | Completed |
| Day 7 | Final Verification & Presentation | Improve cancellation-versus-new-join race test; final regression checks; capture demonstration evidence and prepare presentation | In progress — the cancellation-versus-new-join race still needs a fresh-fixture run (see the verification table) |

## Development Plan — Project 3 iteration (review follow-up)

| Task | Area | Status |
|---|---|---|
| Route café-manager confirm/reject through `BookingService` (shared advisory lock + booking row lock) so it cannot overwrite a concurrent cancellation | Concurrency fix | Done in code; needs a run against PostgreSQL |
| Let a waitlisted user leave the waiting list; clear the waitlist when a session is cancelled or completed | Business rule | Done in code; unit-tested |
| Cancel unanswered `PENDING` bookings after their end time; refuse to confirm ended bookings | Business rule | Done in code; unit-tested |
| Café manager can cancel a `CONFIRMED` booking (cancels the session, notifies participants) | New workflow | Done in code; unit-tested |
| New entities `SessionEvent` and `Notification` (total 17, five new in Project 3) | Data model | Done; ERD updated |
| Session events over SSE (`SESSION_*`), stored notifications, `GET /api/notifications`, `GET /api/study-sessions/{id}/events` | API / real-time | Done in code |
| `CompletableFuture` fan-out (store + push) on `notificationExecutor` | Async | Done; unit-tested |
| JWT `role` claim for the frontend; uniform login / forgot-password answers; rate-limiter cleanup | Security | Done in code; unit-tested where noted in the README |
| Unit tests: bookings, sessions, listener, thread-safety, race demo, JWT; refresh stale `BookingServiceTest` / `CafeManagerBookingServiceTest` | Testing | Written; **not yet executed** (no build environment was available when they were written) |
| Run all tests, then the `*IT` concurrency tests on fresh fixtures, including the cancellation-versus-new-join race | Verification | **Pending** |
| Add Testcontainers PostgreSQL so concurrency tests run in `mvn verify` | Testing | Future |

## Scope and Key Features

**Existing platform:** registration/login, JWT and role-based access, user profiles, jobs and applications, café and study-space management, bookings, account recovery, auditing, and notification functionality.

**Concurrency extension:** group study sessions linked to confirmed bookings, participant registration, capacity enforcement, FIFO waitlists, overlapping-session prevention, coordinated cancellation, and concurrent HTTP integration tests.

## Concurrency Problem and Solution

**Shared resources:** study-session capacity, confirmed participants, waitlist order, and linked booking/session state.

**Risk:** two students joining the last available seat simultaneously could both be accepted; two departures could promote the wrong number of waitlisted students; joining could race with booking cancellation.

**Approach:** transactional service methods and a shared PostgreSQL transaction-scoped advisory lock serialize relevant writes across application instances. Booking operations also use database row locking where appropriate. The current single global advisory lock favors correctness and simplicity, but can reduce throughput by serializing unrelated sessions; finer-grained locks are a future optimization.

**Asynchronous processing:** the existing notification workflow uses an asynchronous executor and transaction-aware event handling so notifications do not unnecessarily block core booking requests. Confirm that new study-session events are covered before claiming additional notification types.

## Verification Evidence

| Scenario | Observed result | Verification status        |
|---|---|----------------------------|
| Two users join a session with one remaining seat | One `JOINED`, one `WAITLISTED` | Passed                     |
| Same user joins two overlapping sessions concurrently | One accepted, other HTTP 409 | Passed                     |
| Two participants leave concurrently | Both requests HTTP 204; follow-up database verification reported correct | Passed                     |
| FIFO waitlist promotion | Database state verified after departures | Passed                     |
| Booking cancellation concurrent with join | Cancellation HTTP 200; join HTTP 409 because joiner had already joined | Passed                     |
| Booking/session final cancellation state | Both statuses reported `CANCELLED` in database verification | Passed |

The concurrency tests exercise the running API against PostgreSQL using `ExecutorService`, `Callable`, `Future`, and `CountDownLatch`. Test fixtures are stateful and should be refreshed before reruns. Do not treat a passing test with an already-joined user as proof of the cancellation-versus-new-join race.

