# WASLAH — User Stories

User stories describe what each kind of user needs and why. Stories US-01 to US-43 come from Units 2–3; US-44 to US-52 complete the role workflows; OPS-01 to OPS-14 cover Unit 4 productionization. The **Service** column shows which microservice delivers each group.

## Product stories

### Account and profile — Identity service

| ID | User story |
| --- | --- |
| US-01 | As a user, I want to register an account, so that I can access the WASLAH platform. |
| US-02 | As a user, I want to log in securely, so that I can access my account. |
| US-03 | As a user, I want to reset my password, so that I can regain access to my account if I forget it. |
| US-04 | As a user, I want to view my profile, so that I can see my personal and professional information. |
| US-05 | As a user, I want to update my profile, so that my information remains up to date. |
| US-06 | As a user, I want to upload a profile image, so that I can personalize my profile. |
| US-44 | As a user, I want to verify my email address, so that the platform knows the account is really mine. |
| US-45 | As a user, I want to change my password while logged in, so that I can keep my account secure. |

### Security — Identity service (applied in every service)

| ID | User story |
| --- | --- |
| US-18 | As a user, I want my account to be protected by authentication, so that unauthorized users cannot access my data. |
| US-19 | As a user, I want sensitive endpoints to require authorization, so that only permitted users can perform certain actions. |
| US-20 | As a system administrator, I want rate limiting on sensitive authentication endpoints, so that excessive requests and abuse can be reduced. |

### Role requests and administration — Identity and Workspace services

| ID | User story |
| --- | --- |
| US-46 | As a user, I want to request an employer account with my company details, so that I can publish jobs after approval. |
| US-47 | As an admin, I want to approve or reject employer requests, so that only genuine companies can publish jobs. |
| US-48 | As a user, I want to request to list my café, so that I can become a café manager after approval. |
| US-49 | As an admin, I want to approve or reject café requests, so that only real cafés appear on the platform. |
| US-50 | As an admin, I want to manage users and see platform statistics and the audit log, so that I can keep the platform safe and understand its use. |

### Jobs — Career service

| ID | User story |
| --- | --- |
| US-07 | As a user, I want to browse available jobs, so that I can find suitable opportunities. |
| US-08 | As a user, I want to view job details, so that I can decide whether a job is suitable for me. |
| US-09 | As a user, I want to apply for suitable jobs, so that I can seek employment opportunities. |
| US-10 | As an employer, I want to create a job posting, so that I can advertise available opportunities. |
| US-11 | As an employer, I want to update my job posting, so that I can keep its information accurate. |
| US-12 | As an employer, I want to close a job posting, so that users do not apply for unavailable positions. |
| US-13 | As an employer, I want to manage job opportunities, so that I can organize my recruitment process. |
| US-51 | As an employer, I want to review the applications for my job and accept or reject them, so that I can fill my vacancies. |
| US-52 | As an employer, I want acceptances to stop when all vacancies are filled, even if I accept two candidates at the same moment, so that I never hire more people than planned. *(concurrency-sensitive)* |

### Bookings — Workspace service

| ID | User story |
| --- | --- |
| US-14 | As a user, I want to create a café booking, so that I can reserve a suitable workspace. |
| US-15 | As a user, I want to view my bookings, so that I can keep track of my reservations. |
| US-16 | As a user, I want to view a booking by its ID, so that I can check its details. |
| US-17 | As a user, I want to cancel my booking, so that I can release a reservation I no longer need. |
| US-21 | As a café manager, I want to view bookings for my café, so that I can manage customer reservations. |
| US-22 | As a café manager, I want to confirm a pending booking, so that customers know their reservation has been accepted. |
| US-23 | As a café manager, I want to reject a pending booking for my café, so that unavailable reservations can be released. |
| US-40 | As a café manager, I want to cancel a confirmed booking, so that I can release a space I can no longer provide; its study session is cancelled and its participants are told. |
| US-41 | As a café manager, I want my confirm or reject action to be coordinated with the customer's cancellation, so that one cannot silently overwrite the other. *(concurrency-sensitive)* |
| US-42 | As a café manager, I want unanswered pending bookings to expire automatically and not be confirmable after they ended, so that stale requests do not block a time slot. |

### Group study sessions and waitlist — Workspace service

| ID | User story |
| --- | --- |
| US-24 | As a booking owner, I want to create a study session from my confirmed booking, so that other students can study with me. |
| US-25 | As an organizer, I want to set a session capacity within the study-space limit, so that I can manage available seats. |
| US-26 | As a student, I want to join a session with an available seat, so that I can participate in group study. |
| US-27 | As a student, I want to enter a FIFO waitlist when a session has no seats, so that I can be considered when a seat becomes available. |
| US-28 | As a participant, I want to leave a session, so that my seat becomes available to another student. |
| US-29 | As a waitlisted student, I want to be promoted when an eligible seat becomes available, so that I can join without submitting another request. |
| US-30 | As a student, I want duplicate and overlapping participation prevented, so that my confirmed sessions do not conflict. |
| US-31 | As an organizer, I want cancellation of my booking to cancel its linked session, so that nobody can join a cancelled session. |
| US-36 | As a waitlisted student, I want to leave the waiting list, so that I am not promoted into a session I no longer want. |
| US-43 | As an organizer or participant, I want to see the history of a session, so that I can understand in what order joins, departures and promotions were applied. *(concurrency-sensitive)* |

### Concurrency and data consistency — Workspace service

| ID | User story |
| --- | --- |
| US-32 | As a student, I want two simultaneous requests for the final seat to be processed safely, so that session capacity is never exceeded. |
| US-33 | As a participant, I want simultaneous departures to promote waiting students without duplicate memberships, so that the waitlist remains consistent. |
| US-34 | As a student, I want simultaneous attempts to join overlapping sessions to be checked safely, so that I cannot receive conflicting confirmed seats. |
| US-35 | As an organizer, I want booking cancellation and session joining to be coordinated, so that cancellation cannot leave an active session accepting new participants. |

### Notifications — Notification service (events from Workspace via Kafka)

| ID | User story |
| --- | --- |
| US-37 | As a student, I want to be told in real time when I am waitlisted or promoted, so that I know whether I have a seat. |
| US-38 | As a user, I want to read notifications I missed while offline, so that I do not lose a promotion or cancellation notice. |
| US-39 | As an organizer, I want to be told when someone joins or leaves my session, so that I know who is attending. |

### Acceptance criteria — key product stories

**Booking without double booking (US-14, OPS-03).** **Given** a study space that is free from 10:00 to 12:00, **when** two users request overlapping slots at the same moment, **then** exactly one booking is created and the other request receives HTTP 409.

**Concurrent seat allocation (US-26, US-27, US-32).** **Given** an OPEN study session with one remaining seat, **when** two different eligible students request to join concurrently, **then** at most one new student receives a confirmed seat and the other is waitlisted; confirmed participants never exceed capacity.

**FIFO promotion (US-29, US-33).** **Given** a FULL session with eligible users waiting, **when** participants leave, **then** available seats are filled from the earliest eligible waitlist positions without duplicate promotion.

**Cancellation (US-31, US-35, US-40).** **Given** a confirmed booking with an active linked session, **when** the booking is cancelled, **then** both booking and session finish as `CANCELLED`, the waitlist is empty, no later join can succeed, and every participant and waitlisted user except the one who cancelled receives `SESSION_CANCELLED`.

**Manager versus customer (US-41).** **Given** a booking the customer is cancelling, **when** the café manager confirms or cancels it at the same moment, **then** the two requests run one after the other: the second sees the new status and receives HTTP 409, and the booking never ends `CONFIRMED` while its session is `CANCELLED`.

**Vacancies (US-52).** **Given** a job with one open vacancy and two pending applications, **when** the employer accepts both at the same moment, **then** only one is accepted and the job shows 1 of 1 vacancies filled.

**Role approval (US-47, US-49).** **Given** a pending employer or café request, **when** an admin approves it, **then** the user's role becomes `EMPLOYER` or `CAFE_MANAGER` and the new permissions apply from the user's next login.

## Productionization stories (Unit 4)

| ID | Priority | User story | Acceptance criteria | Delivered by |
| --- | --- | --- | --- | --- |
| OPS-01 | Must | As a developer, I want each business area to be its own service, so that I can change and deploy one area without the others. | 4 services, each with its own Maven project, Dockerfile, image, port and health check | `services/*` |
| OPS-02 | Must | As a developer, I want to start the whole system with one command, so that anyone can run it the same way. | `docker compose up` starts PostgreSQL, Kafka, 4 services and the frontend; all report healthy | `compose.yaml` |
| OPS-03 | Must | As a user, I want concurrent booking requests handled safely, so that one space cannot be double-booked. | Two overlapping simultaneous requests never create two active bookings; proven by a test on real PostgreSQL | `BookingConcurrencyIT` |
| OPS-04 | Must | As an operator, I want crashed containers to restart by themselves, so that short failures recover without me. | Restart policy and health checks on every container | `compose*.yaml` |
| OPS-05 | Must | As a user, I want my booking to succeed even if the notification part is slow or down, so that one problem does not block everything. | Workspace publishes to Kafka and answers at once; Notification processes the events when it is available | Kafka topics |
| OPS-06 | Must | As a user, I want each notification exactly once, so that I am not confused by duplicates. | Retries plus `processed_events` check; a redelivered event creates no second notification; failing events go to a dead-letter topic | Notification service |
| OPS-07 | Must | As a developer, I want every push built and tested automatically, so that broken code is caught before deployment. | Jenkins builds and tests the 4 services, builds the frontend and the images on every branch; any failure stops the pipeline | `Jenkinsfile` |
| OPS-08 | Must | As an operator, I want versioned images in a registry, so that I know exactly what is running and can go back. | Images in ECR tagged `1.0.<build>` and `sha-<commit>`, never only `latest` | Jenkins + ECR |
| OPS-09 | Must | As an operator, I want deployment to AWS to be automatic but approved, so that releases are fast and controlled. | On `master`, Jenkins pushes images, uploads release files, waits for approval, deploys through SSM and fails if a container is unhealthy | Jenkins + SSM |
| OPS-10 | Must | As an operator, I want to roll back to a previous version, so that a bad release can be undone quickly. | Running the job with `ROLLBACK_VERSION=1.0.x` redeploys that version without rebuilding | `Jenkinsfile` |
| OPS-11 | Must | As the business, I want the database managed and private, so that data is backed up and not exposed to the internet. | Amazon RDS, not publicly accessible, encrypted, TLS required, reachable only from the application server | `setup.sh` |
| OPS-12 | Must | As the business, I want secrets kept out of the code, so that a leaked repository does not expose passwords or keys. | `.env` ignored by Git; secrets in Parameter Store; Jenkins key only in Jenkins credentials | `.gitignore`, Parameter Store |
| OPS-13 | Should | As a user, I want the site served over HTTPS, so that my login and data are protected in transit. | CloudFront HTTPS address; HTTP redirects to HTTPS; the server accepts traffic only from CloudFront | CloudFront, security group |
| OPS-14 | Should | As an operator, I want alerts when the server or database is in trouble or costs rise, so that I can react early. | CloudWatch alarms for CPU, status checks and free storage, and a monthly budget alert, all by email | CloudWatch, SNS, Budgets |
