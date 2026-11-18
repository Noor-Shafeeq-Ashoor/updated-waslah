# WASLAH 4.0 — Updated User Stories

The existing `USER STORY.md` from the earlier projects remains authoritative for product workflows. These **additional** stories cover productionization.

| ID | Priority | User story | Acceptance criteria |
|---|---|---|---|
| OPS-01 | P0 | As an administrator, I want each service independently packaged so one service can be rebuilt without rebuilding all others. | Each has its own Dockerfile, build command and image; targeted service rebuild works. |
| OPS-02 | P0 | As an administrator, I want failed containers to restart so transient crashes recover. | Compose restart policies are set; a controlled container restart recovers and health becomes ready. |
| OPS-03 | P0 | As a user, I want concurrent booking requests handled safely so one space cannot be double-booked. | Two overlapping concurrent requests for one space do not create two active bookings. |
| OPS-04 | P0 | As a user, I want a study-session seat allocated fairly even under concurrency. | Capacity never exceeded; waitlist remains consistent; concurrent integration test passes on PostgreSQL. |
| OPS-05 | P0 | As an administrator, I want every pull request to run automated verification. | Maven verify per service and frontend build run in CI; failed checks are visible. |
| OPS-06 | P0 | As an administrator, I want sensitive credentials outside source control. | `.env` ignored; CI secrets/OIDC used; no credentials in committed docs/screenshots. |
| OPS-07 | P1 | As a user, I want notifications delivered after a booking or session event. | Workspace publishes an event and Notification consumes it; user can read the stored notification. |
| OPS-08 | P1 | As an administrator, I want actionable health and logs. | Health endpoints and Docker logs available; evidence collected without secrets. |
| OPS-09 | P1 | As a release engineer, I want reproducible container images. | Images are tagged and deployment references an explicit version. |
| OPS-10 | P1 | As a developer, I want service-specific API docs. | Swagger/OpenAPI reachable for each running service and protected routes documented. |
| OPS-11 | P1 | As an administrator, I want a documented rollback procedure. | Previous image tag and redeploy commands recorded; rollback tested before claiming completion. |
| OPS-12 | P2 | As a maintainer, I want service-owned data boundaries. | Future migration design replaces cross-service shared-table access with explicit contracts. |

**Status:** Stories express required behavior; an acceptance criterion is not automatically proof it passed. Record evidence alongside each story after execution.
