## API Documentation

WASLAH provides a RESTful API for user management, profiles, jobs, bookings, and café manager operations.

The API is secured using JWT authentication and role-based authorization where required.

> The complete, up-to-date endpoint list is in the [README](./README.md#endpoint-reference) and in Swagger UI (`/swagger-ui/index.html`). The table below is a summary of the core endpoints.

### API Endpoint Reference

| Method | Endpoint                             | Functionality                            | Access             |
| ------ | ------------------------------------ | ---------------------------------------- | ------------------ |
| POST   | `/auth/register`                     | Register a new user                      | Public             |
| GET    | `/auth/test`                         | Test JWT authentication                  | Private            |
| GET    | `/api/profile`                       | Get the authenticated user's profile     | User               |
| PUT    | `/api/profile`                       | Update the authenticated user's profile  | User               |
| PUT    | `/api/profile/image`                 | Update the user's profile image          | User               |
| POST   | `/api/jobs`                          | Create a new job                         | Employer           |
| GET    | `/api/jobs`                          | Get all available jobs                   | Authenticated User |
| GET    | `/api/jobs/{id}`                     | Get a job by ID                          | Authenticated User |
| PUT    | `/api/jobs/{id}`                     | Update a job                             | Employer           |
| DELETE | `/api/jobs/{id}`                     | Close/delete a job                       | Employer           |
| POST   | `/api/bookings`                      | Create a café booking                    | Authenticated User |
| GET    | `/api/bookings`                      | Get the user's bookings                  | Authenticated User |
| GET    | `/api/bookings/{id}`                 | Get a booking by ID                      | Authenticated User |
| PUT    | `/api/bookings/{id}/cancel`          | Cancel a booking                         | Authenticated User |
| GET    | `/api/manager/bookings`              | Get bookings for the café manager's café | Café Manager       |
| PUT    | `/api/manager/bookings/{id}/confirm` | Confirm a booking                        | Café Manager       |
| PUT    | `/api/manager/bookings/{id}/cancel`  | Reject/cancel a booking                  | Café Manager       |

### Authentication

Protected endpoints require a valid JWT access token.

The token should be provided using the HTTP `Authorization` header:

```http
Authorization: Bearer <JWT_TOKEN>
```

Public endpoints do not require authentication.

### Access Levels

* **Public** — No authentication required.
* **Authenticated User** — Requires a valid JWT token.
* **Employer** — Requires a valid JWT token and `EMPLOYER` role.
* **Café Manager** — Requires a valid JWT token and `CAFE_MANAGER` role.

### Swagger / OpenAPI

WASLAH uses Swagger/OpenAPI to provide interactive API documentation.

After starting the application, Swagger UI is available at:

```text
http://localhost:8080/swagger-ui/index.html
```

Swagger provides detailed information about:

* Available API endpoints
* HTTP methods
* Endpoint descriptions
* Path parameters
* Query parameters
* Request bodies
* Response bodies
* HTTP status codes
* Authentication requirements
* JWT Bearer authentication
* Role-based access requirements

### Using JWT Authentication in Swagger

1. Register a user using `/auth/register`.
2. Log in and obtain a JWT token.
3. Open Swagger UI.
4. Click the **Authorize** button.
5. Enter the JWT token using the Bearer authentication scheme.
6. Execute protected endpoints directly from Swagger UI.

Example:

```text
Bearer eyJhbGciOiJIUzI1NiJ9...
```

### API Response Status Codes

Common HTTP status codes used by the API include:

| Status | Meaning                                 |
| ------ | --------------------------------------- |
| 200    | Request completed successfully          |
| 201    | Resource created successfully           |
| 204    | Request completed with no response body |
| 400    | Invalid request or validation error     |
| 401    | Authentication required or invalid      |
| 403    | Access denied                           |
| 404    | Resource not found                      |
| 409    | Conflict                                |
| 429    | Too many requests                       |
| 500    | Internal server error                   |

