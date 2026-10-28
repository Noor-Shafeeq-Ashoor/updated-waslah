
package com.ga.waslah.config;

import com.ga.waslah.model.ApplicationStatus;
import com.ga.waslah.model.Cafe;
import com.ga.waslah.model.Job;
import com.ga.waslah.model.JobApplication;
import com.ga.waslah.model.JobStatus;
import com.ga.waslah.model.JobType;
import com.ga.waslah.model.Role;
import com.ga.waslah.model.StudySpace;
import com.ga.waslah.model.User;
import com.ga.waslah.model.UserProfile;
import com.ga.waslah.model.UserStatus;

import com.ga.waslah.repository.CafeRepository;
import com.ga.waslah.repository.JobApplicationRepository;
import com.ga.waslah.repository.JobRepository;
import com.ga.waslah.repository.StudySpaceRepository;
import com.ga.waslah.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds initial development data for the WASLAH platform.
 *
 * <p>This class creates demonstration accounts, cafés, study spaces,
 * job offers, and job applications.</p>
 *
 * <p>Existing records are checked before inserting new ones,
 * preventing duplicate seed data when the application restarts.</p>
 *
 * <p>This component runs only when the Spring profile is "dev".
 * Bookings and study sessions are not seeded.</p>
 *
 * <p>Demo passwords must be configured for local development only.
 * Never use development credentials in production.</p>
 */
@Slf4j
@Component
@Profile("dev")
public class DataLoader implements CommandLineRunner {

    // =========================================================
    // Repositories
    // =========================================================

    private final UserRepository userRepository;
    private final CafeRepository cafeRepository;
    private final StudySpaceRepository studySpaceRepository;
    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;

    private final PasswordEncoder passwordEncoder;

    // =========================================================
    // Configuration
    // =========================================================

    private final String adminUsername;
    private final String adminEmail;
    private final String adminPassword;
    private final String demoUserPassword;
    private final String cafeManagerPassword;

    /**
     * Constructor injection for repositories and configuration.
     */
    public DataLoader(
            UserRepository userRepository,
            CafeRepository cafeRepository,
            StudySpaceRepository studySpaceRepository,
            JobRepository jobRepository,
            JobApplicationRepository jobApplicationRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.username:admin}") String adminUsername,
            @Value("${app.admin.email:admin@waslah.com}") String adminEmail,
            @Value("${app.admin.password:}") String adminPassword,
            @Value("${app.seed.demo-user-password:}") String demoUserPassword,
            @Value("${app.seed.cafe-manager-password:}") String cafeManagerPassword
    ) {
        this.userRepository = userRepository;
        this.cafeRepository = cafeRepository;
        this.studySpaceRepository = studySpaceRepository;
        this.jobRepository = jobRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.passwordEncoder = passwordEncoder;

        this.adminUsername = adminUsername;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.demoUserPassword = demoUserPassword;
        this.cafeManagerPassword = cafeManagerPassword;
    }

    // =========================================================
    // Application Startup
    // =========================================================

    /**
     * Runs the seed steps when the application starts.
     *
     * @param args application command-line arguments
     */
    @Override
    public void run(String... args) {

        log.info("Starting WASLAH development seed data...");

        // 1. Seed administrator account.
        seedAdmin();

        // 2. Seed verified member accounts.
        // These accounts can be used to test bookings,
        // study sessions, and job applications.
        seedUser(
                "demo.user",
                "demo.user@waslah.com",
                demoUserPassword,
                Role.USER,
                "Demo",
                "User"
        );

        seedUser(
                "organizer1",
                "organizer1@waslah.com",
                demoUserPassword,
                Role.USER,
                "Organizer",
                "One"
        );

        seedUser(
                "organizer2",
                "organizer2@waslah.com",
                demoUserPassword,
                Role.USER,
                "Organizer",
                "Two"
        );

        // 3. Create four student accounts.
        for (int i = 1; i <= 4; i++) {
            seedUser(
                    "student" + i,
                    "student" + i + "@waslah.com",
                    demoUserPassword,
                    Role.USER,
                    "Student",
                    String.valueOf(i)
            );
        }

        // 4. Seed café manager account.
        User cafeManager = seedUser(
                "cafe.manager",
                "cafe.manager@waslah.com",
                cafeManagerPassword,
                Role.CAFE_MANAGER,
                "Cafe",
                "Manager"
        );

        // 5. Seed cafés and study spaces if manager exists.
        if (cafeManager == null) {
            log.warn(
                    "Cafe seed data skipped: configure " +
                            "app.seed.cafe-manager-password."
            );
        } else {

            Cafe manamaCafe = seedCafe(
                    cafeManager,
                    "Bahrain Study Cafe",
                    "A quiet study cafe with comfortable spaces " +
                            "for individual and group work.",
                    "Manama, Bahrain"
            );

            Cafe seefCafe = seedCafe(
                    cafeManager,
                    "WASLAH Workspace Cafe",
                    "A flexible workspace for students, " +
                            "freelancers, and professionals.",
                    "Seef, Bahrain"
            );

            // Manama café study spaces.
            seedStudySpace(
                    manamaCafe,
                    "Quiet Desk 1",
                    1,
                    2.0
            );

            seedStudySpace(
                    manamaCafe,
                    "Quiet Desk 2",
                    1,
                    2.0
            );

            seedStudySpace(
                    manamaCafe,
                    "Group Room",
                    6,
                    8.0
            );

            // Seef café study spaces.
            seedStudySpace(
                    seefCafe,
                    "Individual Desk",
                    1,
                    3.0
            );

            seedStudySpace(
                    seefCafe,
                    "Meeting Room",
                    8,
                    10.0
            );
        }

        // 6. Seed employer, jobs, and pending applications.
        // This runs independently of café seed success.
        seedJobs();

        log.info("WASLAH seed-data initialization completed.");
    }

    // =========================================================
    // Admin Seeding
    // =========================================================

    /**
     * Creates an administrator account if it does not exist.
     *
     * <p>The password must contain at least eight characters
     * and is supplied through application configuration.</p>
     */
    private void seedAdmin() {

        if (adminPassword == null || adminPassword.length() < 8) {
            log.warn(
                    "Admin not seeded: set ADMIN_PASSWORD " +
                            "to at least 8 characters."
            );
            return;
        }

        if (userRepository.existsByUsername(adminUsername)
                || userRepository.existsByEmail(adminEmail)) {

            log.info(
                    "Admin seed skipped because username " +
                            "or email already exists."
            );
            return;
        }

        User admin = new User();

        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setEmailVerified(true);

        userRepository.save(admin);

        log.info("Admin account '{}' created.", adminUsername);
    }

    // =========================================================
    // User Seeding
    // =========================================================

    /**
     * Finds an existing user or creates a new demo account.
     *
     * <p>Existing accounts are not overwritten and their
     * passwords and roles remain unchanged.</p>
     *
     * @param username unique username
     * @param email email address
     * @param rawPassword plain password to encode
     * @param role account role
     * @param firstName user's first name
     * @param lastName user's last name
     * @return existing or newly created user, or null if skipped
     */
    private User seedUser(
            String username,
            String email,
            String rawPassword,
            Role role,
            String firstName,
            String lastName
    ) {

        User existing = userRepository
                .findByUsername(username)
                .orElse(null);

        if (existing != null) {
            return existing;
        }

        existing = userRepository
                .findByEmail(email)
                .orElse(null);

        if (existing != null) {
            log.warn(
                    "Seed user '{}' skipped because its email " +
                            "already belongs to another account.",
                    username
            );
            return null;
        }

        if (rawPassword == null || rawPassword.length() < 8) {
            log.warn(
                    "Seed user '{}' skipped: configure a password " +
                            "of at least 8 characters.",
                    username
            );
            return null;
        }

        User user = new User();

        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);

        // Create the user's profile.
        UserProfile profile = new UserProfile();

        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setBio("WASLAH demonstration account");
        profile.setLocation("Bahrain");

        user.setUserProfile(profile);

        User savedUser = userRepository.save(user);

        log.info(
                "Seed user '{}' created with role {}.",
                username,
                role
        );

        return savedUser;
    }

    // =========================================================
    // Cafe Seeding
    // =========================================================

    /**
     * Finds a café belonging to the specified manager,
     * or creates it if it does not exist.
     */
    private Cafe seedCafe(
            User manager,
            String name,
            String description,
            String location
    ) {

        List<Cafe> managedCafes =
                cafeRepository.findByManager(manager);

        for (Cafe cafe : managedCafes) {

            if (name.equalsIgnoreCase(cafe.getName())) {
                return cafe;
            }
        }

        Cafe cafe = new Cafe();

        cafe.setName(name);
        cafe.setDescription(description);
        cafe.setLocation(location);
        cafe.setActive(true);
        cafe.setManager(manager);

        Cafe savedCafe = cafeRepository.save(cafe);

        log.info("Seed cafe '{}' created.", name);

        return savedCafe;
    }

    // =========================================================
    // Study Space Seeding
    // =========================================================

    /**
     * Finds a study space by café and name,
     * or creates it if missing.
     */
    private StudySpace seedStudySpace(
            Cafe cafe,
            String name,
            int capacity,
            double pricePerHour
    ) {

        for (StudySpace space : studySpaceRepository.findAll()) {

            if (space.getCafe() != null
                    && cafe.getId().equals(space.getCafe().getId())
                    && name.equalsIgnoreCase(space.getName())) {

                return space;
            }
        }

        StudySpace space = new StudySpace();

        space.setName(name);
        space.setCapacity(capacity);
        space.setPricePerHour(pricePerHour);
        space.setAvailable(true);
        space.setCafe(cafe);

        StudySpace savedSpace =
                studySpaceRepository.save(space);

        log.info(
                "Seed study space '{}' created for cafe '{}'.",
                name,
                cafe.getName()
        );

        return savedSpace;
    }

    // =========================================================
    // Job Seeding
    // =========================================================

    /**
     * Seeds a demo employer, five jobs, and pending applications.
     *
     * <p>The Junior Java Developer job has one vacancy,
     * while the Backend Developer job has two vacancies.</p>
     *
     * <p>Four student accounts apply to both jobs so
     * concurrent acceptance can be demonstrated.</p>
     */
    private void seedJobs() {

        // 1. Create the demo employer.
        User employer = seedUser(
                "employer1",
                "employer1@waslah.com",
                demoUserPassword,
                Role.EMPLOYER,
                "Gulf",
                "Tech"
        );

        if (employer == null) {
            log.warn(
                    "Job seed data skipped: employer account " +
                            "could not be created."
            );
            return;
        }

        // Ensure this demo account has the expected role.
        if (employer.getRole() != Role.EMPLOYER) {
            log.warn(
                    "Job seed skipped: employer1 does not " +
                            "have the EMPLOYER role."
            );
            return;
        }

        // 2. Avoid duplicating jobs on restart.
        if (!jobRepository
                .findByCreatedBy_UsernameOrderByCreatedAtDesc(
                        "employer1"
                )
                .isEmpty()) {

            log.info(
                    "Job seed skipped because employer1 " +
                            "already has jobs."
            );
            return;
        }

        // 3. Set future application deadlines.
        LocalDateTime base = LocalDateTime.now()
                .plusDays(45)
                .withNano(0);

        // Job 1: One vacancy.
        Job junior = seedJob(
                employer,
                "Junior Java Developer",
                "Gulf Tech",
                "Manama, Bahrain",
                JobType.FULL_TIME,
                700.0,
                base,
                1,
                "Build and maintain REST APIs with Spring Boot. " +
                        "Great first role for a recent graduate."
        );

        // Job 2: Two vacancies.
        Job backend = seedJob(
                employer,
                "Backend Developer",
                "Gulf Tech",
                "Seef, Bahrain",
                JobType.FULL_TIME,
                950.0,
                base.plusDays(10),
                2,
                "Own services end to end: design, PostgreSQL, " +
                        "testing and deployment."
        );

        // Job 3: Three vacancies.
        seedJob(
                employer,
                "UX Design Intern",
                "Gulf Tech",
                "Manama, Bahrain",
                JobType.INTERNSHIP,
                null,
                base.plusDays(20),
                3,
                "Support the design team with research, " +
                        "wireframes and prototypes."
        );

        // Job 4: Two vacancies.
        seedJob(
                employer,
                "Customer Success Associate",
                "Gulf Tech",
                "Muharraq, Bahrain",
                JobType.PART_TIME,
                400.0,
                base.plusDays(30),
                2,
                "Help customers get the most out of our products. " +
                        "Flexible hours."
        );

        // Job 5: One vacancy.
        seedJob(
                employer,
                "Freelance Technical Writer",
                "Gulf Tech",
                "Remote",
                JobType.FREELANCE,
                300.0,
                base.plusDays(15),
                1,
                "Write clear documentation and tutorials " +
                        "for developers."
        );

        // 4. Seed pending applications.
        for (String student : List.of(
                "student1",
                "student2",
                "student3",
                "student4"
        )) {

            User applicant = userRepository
                    .findByUsername(student)
                    .orElse(null);

            if (applicant == null) {
                log.warn(
                        "Job application seed skipped: " +
                                "user '{}' does not exist.",
                        student
                );
                continue;
            }

            seedApplication(
                    junior,
                    applicant,
                    "I would love to start my career with Gulf Tech."
            );

            seedApplication(
                    backend,
                    applicant,
                    "I have built several Spring Boot projects."
            );
        }

        log.info("Job seed data created successfully.");
    }

    // =========================================================
    // Job Creation Helper
    // =========================================================

    /**
     * Creates and saves a demo job.
     *
     * @param employer owner of the job
     * @param title job title
     * @param company company name
     * @param location job location
     * @param type employment type
     * @param salary offered salary, or null if unspecified
     * @param deadline application deadline
     * @param vacancies total available positions
     * @param description job description
     * @return saved job
     */
    private Job seedJob(
            User employer,
            String title,
            String company,
            String location,
            JobType type,
            Double salary,
            LocalDateTime deadline,
            int vacancies,
            String description
    ) {

        Job job = new Job();

        job.setTitle(title);
        job.setCompanyName(company);
        job.setLocation(location);
        job.setJobType(type);
        job.setSalary(salary);
        job.setDeadline(deadline);
        job.setVacancies(vacancies);
        job.setFilledVacancies(0);
        job.setStatus(JobStatus.OPEN);
        job.setDescription(description);
        job.setCreatedBy(employer);

        Job savedJob = jobRepository.save(job);

        log.info(
                "Seed job '{}' created with {} vacancies.",
                title,
                vacancies
        );

        return savedJob;
    }

    // =========================================================
    // Job Application Seeding
    // =========================================================

    /**
     * Creates a pending application if the user has not
     * already applied for the specified job.
     *
     * @param job target job
     * @param applicant user applying for the job
     * @param coverLetter demonstration cover letter
     */
    private void seedApplication(
            Job job,
            User applicant,
            String coverLetter
    ) {

        // Prevent duplicate applications.
        if (jobApplicationRepository.existsByJobIdAndUserId(
                job.getId(),
                applicant.getId()
        )) {
            return;
        }

        JobApplication application = new JobApplication();

        application.setJob(job);
        application.setUser(applicant);
        application.setStatus(ApplicationStatus.PENDING);
        application.setCoverLetter(coverLetter);

        jobApplicationRepository.save(application);

        log.info(
                "Pending application seeded: user '{}' -> job '{}'.",
                applicant.getUsername(),
                job.getTitle()
        );
    }
}
