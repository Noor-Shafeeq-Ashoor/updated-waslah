# WASLAH 4.0 — Service-specific API Reference

Every endpoint of the four services, taken from the controllers. Each running service also publishes interactive documentation with request and response schemas at `/swagger-ui/index.html` (OpenAPI JSON at `/v3/api-docs`).

| Service | Method | Route / mapping fragment | Controller |
|---|---|---|---|
| identity_service | `GET` | `/api/admin/audit-logs` | `AdminAuditLogController` |
| identity_service | `DELETE` | `/api/admin/users/{id}` | `AdminController` |
| identity_service | `GET` | `/api/admin/users` | `AdminController` |
| identity_service | `PUT` | `/api/admin/users/{id}/reactivate` | `AdminController` |
| identity_service | `GET` | `/api/admin/stats` | `AdminController` |
| identity_service | `GET` | `/api/admin/employer-requests` | `AdminEmployerRequestController` |
| identity_service | `PUT` | `/api/admin/employer-requests/{id}/approve` | `AdminEmployerRequestController` |
| identity_service | `PUT` | `/api/admin/employer-requests/{id}/reject` | `AdminEmployerRequestController` |
| identity_service | `POST` | `/auth/login` | `AuthController` |
| identity_service | `GET` | `/auth/verify-email` | `AuthController` |
| identity_service | `POST` | `/auth/forgot-password` | `AuthController` |
| identity_service | `GET` | `/auth/reset-password` | `AuthController` |
| identity_service | `POST` | `/auth/reset-password` | `AuthController` |
| identity_service | `POST` | `/api/employer-requests` | `EmployerRequestController` |
| identity_service | `GET` | `/api/employer-requests/my` | `EmployerRequestController` |
| identity_service | `GET` | `/api/internal/users/summary` | `InternalUserController` |
| identity_service | `PUT` | `/api/internal/users/{id}/role` | `InternalUserController` |
| identity_service | `POST` | `/auth/register` | `UserController` |
| identity_service | `GET` | `/auth/test` | `UserController` |
| identity_service | `DELETE` | `/api/users/{id}` | `UserManagementController` |
| identity_service | `PUT` | `/api/users/change-password` | `UserManagementController` |
| identity_service | `GET` | `/api/profile` | `UserProfileController` |
| identity_service | `PUT` | `/api/profile` | `UserProfileController` |
| identity_service | `GET` | `/api/profile/image` | `UserProfileController` |
| identity_service | `PUT` | `/api/profile/image` | `UserProfileController` |
| career_service | `GET` | `/api/internal/stats` | `InternalStatsController` |
| career_service | `POST` | `/api/applications/jobs/{jobId}` | `JobApplicationController` |
| career_service | `GET` | `/api/applications/my` | `JobApplicationController` |
| career_service | `GET` | `/api/applications/jobs/{jobId}` | `JobApplicationController` |
| career_service | `PUT` | `/api/applications/{applicationId}/status` | `JobApplicationController` |
| career_service | `POST` | `/api/jobs` | `JobController` |
| career_service | `GET` | `/api/jobs` | `JobController` |
| career_service | `GET` | `/api/jobs/my` | `JobController` |
| career_service | `GET` | `/api/jobs/{id}` | `JobController` |
| career_service | `PUT` | `/api/jobs/{id}` | `JobController` |
| career_service | `DELETE` | `/api/jobs/{id}` | `JobController` |
| career_service | `GET` | `/api/career/whoami` | `WhoAmIController` |
| workspace_service | `POST` | `/api/bookings` | `BookingController` |
| workspace_service | `GET` | `/api/bookings` | `BookingController` |
| workspace_service | `GET` | `/api/bookings/{id}` | `BookingController` |
| workspace_service | `PUT` | `/api/bookings/{id}/confirm` | `BookingController` |
| workspace_service | `PUT` | `/api/bookings/{id}/cancel` | `BookingController` |
| workspace_service | `GET` | `/api/cafes` | `CafeController` |
| workspace_service | `GET` | `/api/cafes/{id}` | `CafeController` |
| workspace_service | `GET` | `/api/cafes/my` | `CafeController` |
| workspace_service | `POST` | `/api/cafes` | `CafeController` |
| workspace_service | `PUT` | `/api/cafes/{id}` | `CafeController` |
| workspace_service | `DELETE` | `/api/cafes/{id}` | `CafeController` |
| workspace_service | `PUT` | `/api/cafes/{id}/activate` | `CafeController` |
| workspace_service | `GET` | `/api/manager/bookings` | `CafeManagerBookingController` |
| workspace_service | `PUT` | `/api/manager/bookings/{id}/confirm` | `CafeManagerBookingController` |
| workspace_service | `PUT` | `/api/manager/bookings/{id}/cancel` | `CafeManagerBookingController` |
| workspace_service | `POST` | `/api/cafes/{cafeId}/study-spaces` | `CafeManagerStudySpaceController` |
| workspace_service | `GET` | `/api/cafes/{cafeId}/study-spaces` | `CafeManagerStudySpaceController` |
| workspace_service | `PUT` | `/api/cafes/{cafeId}/study-spaces/{spaceId}` | `CafeManagerStudySpaceController` |
| workspace_service | `PATCH` | `/api/cafes/{cafeId}/study-spaces/{spaceId}/availability` | `CafeManagerStudySpaceController` |
| workspace_service | `POST` | `/api/cafe-requests` | `CafeRegistrationController` |
| workspace_service | `GET` | `/api/cafe-requests/my` | `CafeRegistrationController` |
| workspace_service | `GET` | `/api/cafe-requests/pending` | `CafeRegistrationController` |
| workspace_service | `GET` | `/api/cafe-requests` | `CafeRegistrationController` |
| workspace_service | `PUT` | `/api/cafe-requests/{id}/decision` | `CafeRegistrationController` |
| workspace_service | `GET` | `/api/internal/stats` | `InternalStatsController` |
| workspace_service | `POST` | `/api/study-sessions` | `StudySessionController` |
| workspace_service | `POST` | `/api/study-sessions/{id}/join` | `StudySessionController` |
| workspace_service | `POST` | `/api/study-sessions/{id}/leave` | `StudySessionController` |
| workspace_service | `GET` | `/api/study-sessions/{id}/events` | `StudySessionController` |
| workspace_service | `GET` | `/api/study-sessions` | `StudySessionController` |
| workspace_service | `GET` | `/api/study-spaces` | `StudySpaceController` |
| workspace_service | `GET` | `/api/career/whoami` | `WhoAmIController` |
| notification_service | `GET` | `/api/notifications/stream` | `NotificationController` |
| notification_service | `GET` | `/api/notifications` | `NotificationController` |
| notification_service | `GET` | `/api/notifications/unread-count` | `NotificationController` |
| notification_service | `PUT` | `/api/notifications/{id}/read` | `NotificationController` |
| notification_service | `GET` | `/api/career/whoami` | `WhoAmIController` |

## Request, response, authentication and status codes

All protected APIs require `Authorization: Bearer <JWT>`; public registration/login/verification/recovery endpoints are exceptions. Role/ownership restrictions vary by controller. Request and response fields and status codes are listed in each service's Swagger UI.

Common error categories: `400` validation, `401` missing/invalid token, `403` forbidden, `404` missing resource, `409` business conflict, `429` rate limit, `500` unexpected failure. Exact applicability varies by endpoint.

### Representative request / response examples

**Register** — `POST http://localhost:8081/auth/register` (public):
```json
{"username":"sara","email":"sara@example.com","password":"Passw0rd!"}
```
**Create booking** — `POST http://localhost:8083/api/bookings` (authenticated):
```json
{"studySpaceId":1,"startTime":"2026-12-01T14:00:00","endTime":"2026-12-01T15:00:00"}
```
Successful booking creation returns a booking response (typically HTTP 201); overlapping reservations must be rejected.

**Standard error example**:
```json
{"timestamp":"2026-11-18T10:30:00","status":404,"error":"RESOURCE_NOT_FOUND","message":"Booking not found","path":"/api/bookings/42"}
```
## Swagger/OpenAPI endpoints (when each service runs locally)

| Service | Swagger UI | OpenAPI JSON |
|---|---|---|
| Identity | http://localhost:8081/swagger-ui/index.html | http://localhost:8081/v3/api-docs |
| Career | http://localhost:8082/swagger-ui/index.html | http://localhost:8082/v3/api-docs |
| Workspace | http://localhost:8083/swagger-ui/index.html | http://localhost:8083/v3/api-docs |
| Notification | http://localhost:8084/swagger-ui/index.html | http://localhost:8084/v3/api-docs |

For Kafka topics and JSON event contracts, see [KAFKA_EVENTS.md](KAFKA_EVENTS.md). Kafka events are not REST endpoints.
