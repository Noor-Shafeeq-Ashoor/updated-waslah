-- WASLAH 4.0: idempotent demo records for a controlled production presentation.
-- Run with psql -X -v ON_ERROR_STOP=1 -v demo_hash='<BCrypt hash>' -f 02-demo-seed.sql
-- Never commit a password or its BCrypt hash to this file.
\if :{?demo_hash}
\else
  \echo 'ERROR: supply -v demo_hash=<BCrypt hash>'
  \quit 1
\endif

BEGIN;
SELECT pg_advisory_xact_lock(84622104);

-- Fail closed if a demo identifier is already owned by a different account.
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM public.users
    WHERE (username = 'waslah_demo_user' AND email <> 'waslah-demo-user@example.com')
       OR (username = 'waslah_demo_employer' AND email <> 'waslah-demo-employer@example.com')
       OR (username = 'waslah_demo_manager' AND email <> 'waslah-demo-manager@example.com')
       OR (email = 'waslah-demo-user@example.com' AND username <> 'waslah_demo_user')
       OR (email = 'waslah-demo-employer@example.com' AND username <> 'waslah_demo_employer')
       OR (email = 'waslah-demo-manager@example.com' AND username <> 'waslah_demo_manager')
  ) THEN
    RAISE EXCEPTION 'Demo username/email collision; no changes made';
  END= IF;
END $$;

-- Passwords are not updated on repeat runs; existing accounts are untouched.
INSERT INTO public.users (username, email, password, role, status, email_verified, failed_login_attempts)
SELECT v.username, v.email, :'demo_hash', v.role, 'ACTIVE', true, 0
FROM (VALUES
  ('waslah_demo_user', 'waslah-demo-user@example.com', 'USER'),
  ('waslah_demo_employer', 'waslah-demo-employer@example.com', 'EMPLOYER'),
  ('waslah_demo_manager', 'waslah-demo-manager@example.com', 'CAFE_MANAGER')
) AS v(username,email,role)
WHERE NOT EXISTS (SELECT 1 FROM public.users u WHERE u.username = v.username);

-- Demo employers: three jobs; jobs created only once.
INSERT INTO public.jobs
(company_name, created_at, deadline, description, job_type, location, salary,
 status, title, created_by, vacancies, filled_vacancies)
SELECT 'WASLAH Demo Technologies', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '60 days',
       v.description, v.job_type, 'Manama, Bahrain', v.salary,
       'OPEN', v.title, u.id, v.vacancies, 0
FROM public.users u
CROSS JOIN (VALUES
  ('Junior Java Developer', 'Build and test Spring Boot REST APIs for a demo hiring workflow.', 'FULL_TIME', 850.0, 2),
  ('Cloud Engineering Intern', 'Support AWS deployment and Docker-based microservices.', 'INTERNSHIP', 450.0, 2),
  ('Frontend React Developer', 'Develop accessible React and TypeScript interfaces.', 'CONTRACT', 700.0, 1)
) AS v(title, description, job_type, salary, vacancies)
WHERE u.username = 'waslah_demo_employer'
  AND NOT EXISTS (SELECT 1 FROM public.jobs j WHERE j.created_by = u.id AND j.title = v.title AND j.company_name = 'WASLAH Demo Technologies');

-- Cafe manager: two partner cafes.
INSERT INTO public.cafes (active, description, location, name, manager_id)
SELECT true, v.description, v.location, v.name, u.id
FROM public.users u
CROSS JOIN (VALUES
  ('WASLAH Demo Cafe - Seef', 'Quiet workspace with reliable Wi-Fi and bookable desks.', 'Seef, Bahrain'),
  ('WASLAH Demo Cafe - Manama', 'Group study rooms and individual desks for students.', 'Manama, Bahrain')
) AS v(name,description,location)
WHERE u.username = 'waslah_demo_manager'
  AND NOT EXISTS (SELECT 1 FROM public.cafes c WHERE c.manager_id = u.id AND c.name = v.name);

-- Bookable spaces.
INSERT INTO public.study_spaces (available, capacity, name, price_per_hour, cafe_id)
SELECT true, v.capacity, v.space_name, v.price, c.id
FROM public.cafes c
JOIN (VALUES
  ('WASLAH Demo Cafe - Seef', 'Quiet Desk A', 1, 2.5),
  ('WASLAH Demo Cafe - Seef', 'Meeting Room', 6, 8.0),
  ('WASLAH Demo Cafe - Manama', 'Study Desk B', 1, 2.0),
  ('WASLAH Demo Cafe - Manama', 'Group Study Room', 8, 10.0)
) AS v(cafe_name,space_name,capacity,price) ON c.name = v.cafe_name
JOIN public.users u ON u.id = c.manager_id AND u.username = 'waslah_demo_manager'
WHERE NOT EXISTS (SELECT 1 FROM public.study_spaces s WHERE s.cafe_id = c.id AND s.name = v.space_name);

-- Sample application (one per job and demo user).
INSERT INTO public.job_applications (applied_at, cover_letter, status, job_id, user_id)
SELECT CURRENT_TIMESTAMP, 'Demo application for the WASLAH presentation.', 'PENDING', j.id, u.id
FROM public.jobs j
JOIN public.users e ON e.id = j.created_by AND e.username = 'waslah_demo_employer'
JOIN public.users u ON u.username = 'waslah_demo_user'
WHERE j.title = 'Junior Java Developer' AND j.company_name = 'WASLAH Demo Technologies'
  AND NOT EXISTS (SELECT 1 FROM public.job_applications a WHERE a.job_id = j.id AND a.user_id = u.id);

-- One completed booking in the past, to avoid blocking live booking demonstrations.
INSERT INTO public.bookings (created_at, start_time, end_time, status, updated_at, study_space_id, user_id)
SELECT CURRENT_TIMESTAMP - INTERVAL '8 days',
       (CURRENT_DATE - INTERVAL '7 days') + INTERVAL '10 hours',
       (CURRENT_DATE - INTERVAL '7 days') + INTERVAL '11 hours',
       'COMPLETED', CURRENT_TIMESTAMP - INTERVAL '7 days', s.id, u.id
FROM public.study_spaces s
JOIN public.cafes c ON c.id = s.cafe_id AND c.name = 'WASLAH Demo Cafe - Seef'
JOIN public.users m ON m.id = c.manager_id AND m.username = 'waslah_demo_manager'
JOIN public.users u ON u.username = 'waslah_demo_user'
WHERE s.name = 'Quiet Desk A'
  AND NOT EXISTS (
    SELECT 1 FROM public.bookings b
    WHERE b.study_space_id = s.id AND b.user_id = u.id AND b.status = 'COMPLETED'
      AND b.start_time = (CURRENT_DATE - INTERVAL '7 days') + INTERVAL '10 hours'
  );

-- Example completed study session attached to the demo booking.
INSERT INTO public.study_sessions (capacity, created_at, description, status, title, booking_id, organizer_id)
SELECT 1, b.created_at, 'Past demonstration session.', 'COMPLETED',
       'WASLAH Demo Study Session', b.id, u.id
FROM public.bookings b
JOIN public.users u ON u.id = b.user_id AND u.username = 'waslah_demo_user'
JOIN public.study_spaces s ON s.id = b.study_space_id AND s.name = 'Quiet Desk A'
JOIN public.cafes c ON c.id = s.cafe_id AND c.name = 'WASLAH Demo Cafe - Seef'
WHERE b.status = 'COMPLETED'
  AND NOT EXISTS (SELECT 1 FROM public.study_sessions ss WHERE ss.booking_id = b.id);

-- Notifications are intentionally not inserted until the application's
-- NotificationType enum has been verified; Kafka events must be generated by APIs.

COMMIT;

SELECT 'demo_users' AS item, count(*) AS count FROM public.users WHERE username LIKE 'waslah_demo_%'
UNION ALL SELECT 'demo_jobs', count(*) FROM public.jobs WHERE company_name = 'WASLAH Demo Technologies'
UNION ALL SELECT 'demo_cafes', count(*) FROM public.cafes WHERE name LIKE 'WASLAH Demo Cafe - %';
