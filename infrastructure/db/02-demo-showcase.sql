-- WASLAH 4.0 showcase seed, based on the uploaded 01-schema.pg17.sql.
-- Run ONLY against a disposable clone first. Never commit password/hash.
-- psql -X -v ON_ERROR_STOP=1 -v demo_hash='<BCrypt hash>' -f 02-demo-showcase.sql
\if :{?demo_hash}
\else
  \echo 'ERROR: supply -v demo_hash=<BCrypt hash> generated securely outside Git'
  \quit 1
\endif
BEGIN;
SELECT pg_advisory_xact_lock(84622105);

-- Dedicated synthetic accounts only; existing non-demo accounts remain untouched.
CREATE TEMP TABLE demo_accounts (username text PRIMARY KEY, email text NOT NULL, role text NOT NULL, first_name text NOT NULL, last_name text NOT NULL, bio text NOT NULL) ON COMMIT DROP;
INSERT INTO demo_accounts VALUES
('demo_sara','sara.ahmed@demo.waslah.example','USER','Sara','Ahmed','Cybersecurity student interested in cloud computing.'),
('demo_ali','ali.hassan@demo.waslah.example','USER','Ali','Hassan','Java developer and study-group organizer.'),
('demo_fatima','fatima.khalid@demo.waslah.example','USER','Fatima','Khalid','Frontend developer learning React.'),
('demo_omar','omar.mohammed@demo.waslah.example','USER','Omar','Mohammed','Software engineering student.'),
('demo_ahmed_hr','ahmed.hr@demo.waslah.example','EMPLOYER','Ahmed','Al Khalifa','Recruiter at Pearl Technology.'),
('demo_maryam_hr','maryam.hr@demo.waslah.example','EMPLOYER','Maryam','Yousif','Hiring manager at Gulf Digital.'),
('demo_khalid_hr','khalid.hr@demo.waslah.example','EMPLOYER','Khalid','Salman','Talent partner at Bahrain Cloud Labs.'),
('demo_seef_mgr','layla.cafe@demo.waslah.example','CAFE_MANAGER','Layla','Hassan','Seef workspace manager.'),
('demo_manama_mgr','yousif.cafe@demo.waslah.example','CAFE_MANAGER','Yousif','Ahmed','Manama workspace manager.'),
('demo_juffair_mgr','reem.cafe@demo.waslah.example','CAFE_MANAGER','Reem','Ali','Juffair workspace manager.'),
('demo_muharraq_mgr','huda.cafe@demo.waslah.example','CAFE_MANAGER','Huda','Khalid','Muharraq workspace manager.'),
('demo_showcase_admin','admin.showcase@demo.waslah.example','ADMIN','WASLAH','Admin','Showcase administrator.');

DO $$ BEGIN
 IF EXISTS (SELECT 1 FROM demo_accounts d JOIN public.users u ON u.username=d.username WHERE u.email<>d.email OR u.role<>d.role)
 OR EXISTS (SELECT 1 FROM demo_accounts d JOIN public.users u ON u.email=d.email WHERE u.username<>d.username)
 THEN RAISE EXCEPTION 'Demo username/email collision; no changes made'; END IF;
END $$;

-- Only these specifically named demo accounts receive the supplied hash.
INSERT INTO public.users(username,email,password,role,status,email_verified,failed_login_attempts)
SELECT username,email,:'demo_hash',role,'ACTIVE',true,0 FROM demo_accounts
ON CONFLICT (username) DO NOTHING;
UPDATE public.users u SET password=:'demo_hash',status='ACTIVE',email_verified=true,failed_login_attempts=0,locked_until=NULL
FROM demo_accounts d WHERE u.username=d.username AND u.email=d.email AND u.role=d.role;

-- Profile images are deliberately NULL: profile_image is a local file path, not a public URL.
INSERT INTO public.user_profiles(first_name,last_name,bio,location,profile_image)
SELECT d.first_name,d.last_name,d.bio,'Bahrain',NULL
FROM demo_accounts d JOIN public.users u ON u.username=d.username WHERE u.profile_id IS NULL;
-- Link newly created profiles by stable identifying fields; avoid reusing another user's profile.
UPDATE public.users u SET profile_id=p.id
FROM demo_accounts d, public.user_profiles p
WHERE u.username=d.username AND u.profile_id IS NULL
AND p.first_name=d.first_name AND p.last_name=d.last_name AND p.bio=d.bio
AND NOT EXISTS (SELECT 1 FROM public.users existing WHERE existing.profile_id=p.id);

CREATE TEMP TABLE demo_cafes(name text PRIMARY KEY, location text, description text, manager text) ON COMMIT DROP;
INSERT INTO demo_cafes VALUES
('WASLAH Seef Study Hub','Seef, Bahrain','Quiet desks, fast Wi-Fi and collaborative study rooms.','demo_seef_mgr'),
('WASLAH Manama Learning Lounge','Manama, Bahrain','Comfortable group tables and study-friendly ambience.','demo_manama_mgr'),
('WASLAH Juffair Focus Cafe','Juffair, Bahrain','Calm workspace for focused coding sessions.','demo_juffair_mgr'),
('WASLAH Muharraq Creative Corner','Muharraq, Bahrain','Bright creative workspace for students and teams.','demo_muharraq_mgr');
INSERT INTO public.cafes(active,description,location,name,manager_id)
SELECT true,d.description,d.location,d.name,u.id FROM demo_cafes d JOIN public.users u ON u.username=d.manager
WHERE NOT EXISTS (SELECT 1 FROM public.cafes c WHERE c.name=d.name);

CREATE TEMP TABLE demo_spaces(cafe text, name text, capacity int, price double precision) ON COMMIT DROP;
INSERT INTO demo_spaces VALUES
('WASLAH Seef Study Hub','Seef Group Room',6,8.0),('WASLAH Seef Study Hub','Seef Quiet Desk',1,2.5),
('WASLAH Manama Learning Lounge','Manama Workshop Room',8,10.0),('WASLAH Manama Learning Lounge','Manama Study Table',4,5.0),
('WASLAH Juffair Focus Cafe','Juffair Coding Room',5,7.0),('WASLAH Juffair Focus Cafe','Juffair Focus Desk',1,2.0),
('WASLAH Muharraq Creative Corner','Muharraq Collaboration Room',6,8.5),('WASLAH Muharraq Creative Corner','Muharraq Reading Table',4,4.0);
INSERT INTO public.study_spaces(available,capacity,name,price_per_hour,cafe_id)
SELECT true,d.capacity,d.name,d.price,c.id FROM demo_spaces d JOIN public.cafes c ON c.name=d.cafe
WHERE NOT EXISTS (SELECT 1 FROM public.study_spaces s WHERE s.cafe_id=c.id AND s.name=d.name);

CREATE TEMP TABLE demo_jobs(employer text, company text, title text, description text, kind text, salary double precision, vacancies int) ON COMMIT DROP;
INSERT INTO demo_jobs VALUES
('demo_ahmed_hr','Pearl Technology','Junior Java Developer','Develop secure Spring Boot REST APIs.','FULL_TIME',850,2),
('demo_ahmed_hr','Pearl Technology','QA Automation Engineer','Build automated integration tests.','FULL_TIME',800,2),
('demo_ahmed_hr','Pearl Technology','React Developer','Build accessible frontend applications.','CONTRACT',700,1),
('demo_maryam_hr','Gulf Digital','Cybersecurity Analyst','Support security monitoring and assessments.','FULL_TIME',950,2),
('demo_maryam_hr','Gulf Digital','UX Design Intern','Research and prototype user journeys.','INTERNSHIP',400,2),
('demo_maryam_hr','Gulf Digital','Business Analyst','Translate business needs into product requirements.','PART_TIME',600,1),
('demo_khalid_hr','Bahrain Cloud Labs','AWS Cloud Intern','Assist with cloud deployments and infrastructure.','INTERNSHIP',500,3),
('demo_khalid_hr','Bahrain Cloud Labs','DevOps Engineer','Maintain CI/CD and container workloads.','FULL_TIME',1100,1),
('demo_khalid_hr','Bahrain Cloud Labs','Database Developer','Optimize PostgreSQL queries and schema.','FREELANCE',650,2);
INSERT INTO public.jobs(company_name,created_at,deadline,description,job_type,location,salary,status,title,created_by,vacancies,filled_vacancies)
SELECT d.company,now(),now()+interval '60 days',d.description,d.kind,'Bahrain',d.salary,'OPEN',d.title,u.id,d.vacancies,0
FROM demo_jobs d JOIN public.users u ON u.username=d.employer
WHERE NOT EXISTS(SELECT 1 FROM public.jobs j WHERE j.created_by=u.id AND j.title=d.title AND j.company_name=d.company);

CREATE TEMP TABLE demo_applications(username text, job text, status text) ON COMMIT DROP;
INSERT INTO demo_applications VALUES
('demo_sara','Junior Java Developer','PENDING'),('demo_ali','Junior Java Developer','ACCEPTED'),
('demo_fatima','React Developer','PENDING'),('demo_omar','QA Automation Engineer','REJECTED'),
('demo_sara','Cybersecurity Analyst','ACCEPTED'),('demo_ali','AWS Cloud Intern','PENDING'),
('demo_fatima','UX Design Intern','PENDING'),('demo_omar','Database Developer','PENDING'),
('demo_sara','DevOps Engineer','PENDING'),('demo_ali','Business Analyst','REJECTED'),
('demo_fatima','AWS Cloud Intern','PENDING'),('demo_omar','Cybersecurity Analyst','PENDING');
INSERT INTO public.job_applications(applied_at,cover_letter,status,job_id,user_id)
SELECT now()-interval '2 days','WASLAH showcase application.',a.status,j.id,u.id
FROM demo_applications a JOIN public.users u ON u.username=a.username
JOIN public.jobs j ON j.title=a.job JOIN demo_jobs d ON d.title=j.title AND d.company=j.company_name
WHERE NOT EXISTS(SELECT 1 FROM public.job_applications x WHERE x.job_id=j.id AND x.user_id=u.id);

-- Every session belongs to a confirmed future booking; organizer counts as participant.
-- Relative dates keep the demo usable on the next day; reruns preserve existing bookings.
CREATE TEMP TABLE demo_sessions(title text PRIMARY KEY, cafe text, space text, organizer text, day_offset int, start_hour int, capacity int, status text, description text) ON COMMIT DROP;
INSERT INTO demo_sessions VALUES
('Java Bootcamp Study Group','WASLAH Seef Study Hub','Seef Group Room','demo_ali',1,10,3,'FULL','All seats filled.'),
('AWS Cloud Workshop','WASLAH Manama Learning Lounge','Manama Workshop Room','demo_sara',1,14,2,'FULL','Full session with an active waitlist.'),
('Cybersecurity Revision','WASLAH Juffair Focus Cafe','Juffair Coding Room','demo_fatima',2,10,3,'OPEN','One seat remaining for a concurrency demonstration.'),
('React Study Circle','WASLAH Muharraq Creative Corner','Muharraq Collaboration Room','demo_omar',2,14,4,'OPEN','Open group for React practice.'),
('Database Study Session','WASLAH Seef Study Hub','Seef Group Room','demo_sara',3,10,3,'CANCELLED','Cancelled example.'),
('Spring Boot Practice','WASLAH Manama Learning Lounge','Manama Workshop Room','demo_ali',-7,10,3,'COMPLETED','Completed historical example.');

INSERT INTO public.bookings(created_at,start_time,end_time,status,updated_at,study_space_id,user_id)
SELECT now(),(current_date+s.day_offset)+make_interval(hours=>s.start_hour),
(current_date+s.day_offset)+make_interval(hours=>s.start_hour+2),
CASE WHEN s.status='CANCELLED' THEN 'CANCELLED' WHEN s.status='COMPLETED' THEN 'COMPLETED' ELSE 'CONFIRMED' END,
now(),sp.id,u.id
FROM demo_sessions s JOIN public.cafes c ON c.name=s.cafe JOIN public.study_spaces sp ON sp.cafe_id=c.id AND sp.name=s.space
JOIN public.users u ON u.username=s.organizer
WHERE NOT EXISTS(SELECT 1 FROM public.study_sessions ss WHERE ss.title=s.title AND ss.organizer_id=u.id)
AND NOT EXISTS(SELECT 1 FROM public.bookings b WHERE b.study_space_id=sp.id AND b.user_id=u.id
AND b.start_time=(current_date+s.day_offset)+make_interval(hours=>s.start_hour));

INSERT INTO public.study_sessions(capacity,created_at,description,status,title,updated_at,booking_id,organizer_id)
SELECT s.capacity,now(),s.description,s.status,s.title,now(),b.id,u.id
FROM demo_sessions s JOIN public.users u ON u.username=s.organizer
JOIN public.cafes c ON c.name=s.cafe JOIN public.study_spaces sp ON sp.cafe_id=c.id AND sp.name=s.space
JOIN public.bookings b ON b.study_space_id=sp.id AND b.user_id=u.id
AND b.start_time=(current_date+s.day_offset)+make_interval(hours=>s.start_hour)
WHERE NOT EXISTS(SELECT 1 FROM public.study_sessions ss WHERE ss.title=s.title AND ss.organizer_id=u.id);

CREATE TEMP TABLE demo_members(session_title text, username text) ON COMMIT DROP;
INSERT INTO demo_members VALUES
('Java Bootcamp Study Group','demo_ali'),('Java Bootcamp Study Group','demo_sara'),('Java Bootcamp Study Group','demo_fatima'),
('AWS Cloud Workshop','demo_sara'),('AWS Cloud Workshop','demo_ali'),
('Cybersecurity Revision','demo_fatima'),('Cybersecurity Revision','demo_sara'),
('React Study Circle','demo_omar'),('React Study Circle','demo_ali'),
('Database Study Session','demo_sara'),('Spring Boot Practice','demo_ali'),('Spring Boot Practice','demo_omar');
INSERT INTO public.session_participants(joined_at,session_id,user_id)
SELECT now(),s.id,u.id FROM demo_members d JOIN public.study_sessions s ON s.title=d.session_title
JOIN public.users u ON u.username=d.username
WHERE NOT EXISTS(SELECT 1 FROM public.session_participants p WHERE p.session_id=s.id AND p.user_id=u.id);

CREATE TEMP TABLE demo_waitlist(session_title text, username text, pos int) ON COMMIT DROP;
INSERT INTO demo_waitlist VALUES ('AWS Cloud Workshop','demo_fatima',1),('AWS Cloud Workshop','demo_omar',2);
INSERT INTO public.session_waitlist(joined_at,"position",session_id,user_id)
SELECT now(),d.pos,s.id,u.id FROM demo_waitlist d JOIN public.study_sessions s ON s.title=d.session_title
JOIN public.users u ON u.username=d.username
WHERE NOT EXISTS(SELECT 1 FROM public.session_waitlist w WHERE w.session_id=s.id AND (w.user_id=u.id OR w."position"=d.pos));

-- Do not fabricate Kafka notifications/events; exercise those flows through real APIs.
COMMIT;
SELECT 'demo_accounts' item,count(*) FROM public.users WHERE username IN (SELECT username FROM (VALUES
('demo_sara'),('demo_ali'),('demo_fatima'),('demo_omar'),('demo_ahmed_hr'),('demo_maryam_hr'),('demo_khalid_hr'),('demo_seef_mgr'),('demo_manama_mgr'),('demo_juffair_mgr'),('demo_muharraq_mgr'),('demo_showcase_admin')) t(username))
UNION ALL SELECT 'demo_cafes',count(*) FROM public.cafes WHERE name LIKE 'WASLAH %'
UNION ALL SELECT 'demo_sessions',count(*) FROM public.study_sessions WHERE title IN ('Java Bootcamp Study Group','AWS Cloud Workshop','Cybersecurity Revision','React Study Circle','Database Study Session','Spring Boot Practice');
