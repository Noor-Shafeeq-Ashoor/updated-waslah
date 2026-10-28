# WASLAH — User Stories

## User Stories

The following user stories define the main users of the WASLAH platform and the functionality they need.

### User / Job Seeker

| ID    | User Story                                                                                        |
| ----- | ------------------------------------------------------------------------------------------------- |
| US-01 | As a user, I want to register an account, so that I can access the WASLAH platform.               |
| US-02 | As a user, I want to log in securely, so that I can access my account.                            |
| US-03 | As a user, I want to reset my password, so that I can regain access to my account if I forget it. |
| US-04 | As a user, I want to view my profile, so that I can see my personal and professional information. |
| US-05 | As a user, I want to update my profile, so that my information remains up to date.                |
| US-06 | As a user, I want to upload a profile image, so that I can personalize my profile.                |
| US-07 | As a user, I want to browse available jobs, so that I can find suitable opportunities.            |
| US-08 | As a user, I want to view job details, so that I can decide whether a job is suitable for me.     |
| US-09 | As a user, I want to apply for suitable jobs, so that I can seek employment opportunities.        |

### Employer

| ID    | User Story                                                                                           |
| ----- | ---------------------------------------------------------------------------------------------------- |
| US-10 | As an employer, I want to create a job posting, so that I can advertise available opportunities.     |
| US-11 | As an employer, I want to update my job posting, so that I can keep its information accurate.        |
| US-12 | As an employer, I want to close a job posting, so that users do not apply for unavailable positions. |
| US-13 | As an employer, I want to manage job opportunities, so that I can organize my recruitment process.   |

### Café / Workspace Booking

| ID    | User Story                                                                                    |
| ----- | --------------------------------------------------------------------------------------------- |
| US-14 | As a user, I want to create a café booking, so that I can reserve a suitable workspace.       |
| US-15 | As a user, I want to view my bookings, so that I can keep track of my reservations.           |
| US-16 | As a user, I want to view a booking by its ID, so that I can check its details.               |
| US-17 | As a user, I want to cancel my booking, so that I can release a reservation I no longer need. |

### Security

| ID    | User Story                                                                                                                                  |
| ----- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| US-18 | As a user, I want my account to be protected by authentication, so that unauthorized users cannot access my data.                           |
| US-19 | As a user, I want sensitive endpoints to require authorization, so that only permitted users can perform certain actions.                   |
| US-20 | As a system administrator, I want rate limiting on sensitive authentication endpoints, so that excessive requests and abuse can be reduced. |

### Café Manager

| ID | User Story |
|---|---|
| US-21 | As a café manager, I want to view bookings for my café, so that I can manage customer reservations. |
| US-22 | As a café manager, I want to confirm a pending booking, so that customers know their reservation has been accepted. |
| US-23 | As a café manager, I want to reject a pending booking for my café, so that unavailable reservations can be released. |

### Group Study Sessions & Waitlist

| ID | User Story |
|---|---|
| US-24 | As a booking owner, I want to create a study session from my confirmed booking, so that other students can study with me. |
| US-25 | As an organizer, I want to set a session capacity within the study-space limit, so that I can manage available seats. |
| US-26 | As a student, I want to join a session with an available seat, so that I can participate in group study. |
| US-27 | As a student, I want to enter a FIFO waitlist when a session has no seats, so that I can be considered when a seat becomes available. |
| US-28 | As a participant, I want to leave a session, so that my seat becomes available to another student. |
| US-29 | As a waitlisted student, I want to be promoted when an eligible seat becomes available, so that I can join without submitting another request. |
| US-30 | As a student, I want duplicate and overlapping participation prevented, so that my confirmed sessions do not conflict. |
| US-31 | As an organizer, I want cancellation of my booking to cancel its linked session, so that nobody can join a cancelled session. |

### Concurrency & Data Consistency

| ID | User Story |
|---|---|
| US-32 | As a student, I want two simultaneous requests for the final seat to be processed safely, so that session capacity is never exceeded. |
| US-33 | As a participant, I want simultaneous departures to promote waiting students without duplicate memberships, so that the waitlist remains consistent. |
| US-34 | As a student, I want simultaneous attempts to join overlapping sessions to be checked safely, so that I cannot receive conflicting confirmed seats. |
| US-35 | As an organizer, I want booking cancellation and session joining to be coordinated, so that cancellation cannot leave an active session accepting new participants. |

### Waitlist, Notifications and Booking Management (Project 3 iteration)

| ID | User Story |
|---|---|
| US-36 | As a waitlisted student, I want to leave the waiting list, so that I am not promoted into a session I no longer want. |
| US-37 | As a student, I want to be told in real time when I am waitlisted or promoted, so that I know whether I have a seat. |
| US-38 | As a user, I want to read notifications I missed while offline, so that I do not lose a promotion or cancellation notice. |
| US-39 | As an organizer, I want to be told when someone joins or leaves my session, so that I know who is attending. |
| US-40 | As a café manager, I want to cancel a confirmed booking, so that I can release a space I can no longer provide; its study session is cancelled and its participants are told. |
| US-41 | As a café manager, I want my confirm or reject action to be coordinated with the customer's cancellation, so that one cannot silently overwrite the other. *(concurrency-sensitive)* |
| US-42 | As a café manager, I want unanswered pending bookings to expire automatically and not be confirmable after they ended, so that stale requests do not block a time slot. |
| US-43 | As an organizer or participant, I want to see the history of a session, so that I can understand in what order joins, departures and promotions were applied. *(concurrency-sensitive)* |

### Acceptance Criteria — Concurrent Seat Allocation

**Given** an OPEN study session with one remaining seat, **when** two different eligible students request to join concurrently, **then** at most one new student receives a confirmed seat and the other is waitlisted; confirmed participants never exceed capacity.

**Given** a FULL session with eligible users waiting, **when** participants leave, **then** available seats are filled from the earliest eligible waitlist positions without duplicate promotion.

**Given** a confirmed booking with an active linked session, **when** the booking is cancelled, **then** both booking and linked session finish as CANCELLED, and no later join may succeed.

**Given** a confirmed booking that the customer is cancelling, **when** the café manager cancels or confirms the same booking at the same moment, **then** both requests are serialized by the shared lock: the second one sees the new status and receives HTTP 409, and the booking can never end up `CONFIRMED` while its session is `CANCELLED`.

**Given** a session with a waiting list, **when** its booking is cancelled, **then** the session is `CANCELLED`, the waiting list is empty, and every participant and waitlisted user (except whoever cancelled) receives `SESSION_CANCELLED`.

**Testing note:** The cancellation-versus-new-join race still requires a fresh-fixture test with a joiner who has not already joined; do not claim this particular race is fully verified.
