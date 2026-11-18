# WASLAH 4.0 — Productionization Plan and Progress

| Phase | Work | Evidence / status |
|---|---|---|
| Day 1 — Architecture | Identify Identity/Career/Workspace/Notification boundaries; decide JWT, no gateway, shared PostgreSQL | Four service folders present in repository snapshot |
| Day 2 — Extraction | Migrate controllers/services/entities, remove monolith dependencies, configure service ports | Four independent Spring Boot projects present |
| Day 3 — Docker | Dockerfiles, Compose, service networking, healthchecks, persistent volumes | Compose and Dockerfiles present; runtime verification required |
| Day 4 — Kafka | Producer, two topics, consumer, notification event flow | Producer/consumer/topic config present; end-to-end runtime verification required |
| Day 5 — Testing | Maven tests, BDD, Testcontainers PostgreSQL concurrency | Workspace Maven verify success reported; verify other service CI jobs |
| Day 6 — AWS | ECR, EC2, S3, Systems Manager, IAM/OIDC, production Compose | Infrastructure scripts/workflows present; AWS live deployment evidence pending |
| Day 7 — CI/CD | GitHub Actions/Jenkins builds, image publishing, deployment checks | Workflow definitions present; confirm green run and deployment |
| Final — Documentation | README, architecture, updated ERD, user stories, API reference, screenshots, presentation | Documentation package prepared; screenshots and live checks still pending |

## Remaining submission actions
1. Run/inspect the current GitHub Actions workflow and record its URL and conclusion.
2. Confirm Docker Compose starts all services; capture `docker compose ps` and readiness checks.
3. Confirm Kafka end-to-end message publication and stored notification delivery.
4. Verify AWS instance, running endpoints and resource screenshots (redact secrets).
5. Validate Swagger endpoints, reconcile ERD against the live schema and commit final docs.
6. Add dated evidence references, then push final documentation commit.
