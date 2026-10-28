# WASLAH frontend

React + TypeScript + Vite frontend integrated with the existing Spring Boot WASLAH API. Warm cream/mocha design, responsive sidebar, public landing page, authentication, jobs, cafés and reservations, study sessions and FIFO waitlist, profiles, and role-based management entry points.

## Run locally

1. Start PostgreSQL and the Spring Boot backend on port **8080**. Set `WASLAH_SEED_CONCURRENCY_TESTS=false` for normal use.
2. From `frontend/` run `npm install` then `npm run dev`.
3. Open http://localhost:5173 . The Vite proxy sends `/api` and `/auth` requests to port 8080, avoiding a local CORS change.
4. Run `npm run build` to type-check and build the frontend.

**Do not commit secrets**. JWTs are stored in browser `sessionStorage` for this demo, not localStorage. For production, use a secure cookie/BFF authentication design and CSRF protections as appropriate. Role-based UI is not authorization; Spring Security must enforce all permissions.

## Backend compatibility

The existing backend had POST create/join/leave study-session endpoints but no GET listing endpoint. This package adds `GET /api/study-sessions` to the backend service/controller, returning the existing `StudySessionResponse` DTO. Public browsing of jobs/cafés/sessions is represented visually on the landing page; the current backend requires authentication for `/api/**`, so the real list pages are protected.

Some management workflows (full café CRUD, study-space management, all admin tools, live notification UI) are not yet surfaced in the React UI; their existing backend endpoints remain available through Swagger. No demo records are fabricated: empty states are shown when the database has no results. Live capacity counts and waitlist positions are not available from the current study-session listing DTO; the join action uses the backend's `JOINED`/`WAITLISTED` response. The session list polls every 15 seconds. External Unsplash photos require internet access.

**Verification:** `npm run build` verifies frontend TypeScript and bundling. Full end-to-end flows require a running backend, valid users, and suitable database fixtures.

## Role-based areas
- Member (`USER`): `/dashboard`, jobs, applications, cafés, bookings, study sessions, partner requests.
- Admin: `/admin` (stats), `/admin/users`, `/admin/requests`, `/admin/audit`.
- Employer: `/employer`, `/employer/jobs` (jobs and applicants).
- Café manager: `/manager`, `/manager/spaces`, `/manager/bookings`.
Route guards live in `src/auth.tsx`; the backend `@PreAuthorize` rules remain the real protection.
Only a confirmed booking, still in the future, in a space with 2 or more seats can start a study session.
After an admin approves a role request the user signs in again to get the new role.
