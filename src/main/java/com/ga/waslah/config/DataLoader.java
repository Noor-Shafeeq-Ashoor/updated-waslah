package com.ga.waslah.config;

import com.ga.waslah.model.Cafe;
import com.ga.waslah.model.Role;
import com.ga.waslah.model.StudySpace;
import com.ga.waslah.model.User;
import com.ga.waslah.model.UserProfile;
import com.ga.waslah.model.UserStatus;
import com.ga.waslah.repository.CafeRepository;
import com.ga.waslah.repository.StudySpaceRepository;
import com.ga.waslah.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * Seeds initial development data so the application can be demonstrated
 * without manually creating every user, cafe, or study space.
 *
 * The seeding methods check for existing records before inserting them,
 * so restarting the application does not repeatedly duplicate the seed data.
 *
 * <p>Only runs when the Spring profile is dev. Does not seed bookings or sessions.
 * Demo passwords should be configured only for local development.
 * Do not use the development credentials in a deployed environment.</p>
 */
@Slf4j
@Component
@Profile("dev")
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CafeRepository cafeRepository;
    private final StudySpaceRepository studySpaceRepository;
    private final PasswordEncoder passwordEncoder;

    private final String adminUsername;
    private final String adminEmail;
    private final String adminPassword;
    private final String demoUserPassword;
    private final String cafeManagerPassword;

    public DataLoader(
            UserRepository userRepository,
            CafeRepository cafeRepository,
            StudySpaceRepository studySpaceRepository,
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
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.demoUserPassword = demoUserPassword;
        this.cafeManagerPassword = cafeManagerPassword;
    }

    /**
     * Runs the seed steps when the Spring Boot application starts.
     *
     * @param args command-line arguments supplied to the application
     */
    @Override
    public void run(String... args) {
        seedAdmin();

        // Dedicated verified accounts for end-to-end session testing.
        // No bookings or sessions are seeded: exercise those APIs in Postman.
        seedUser("demo.user", "demo.user@waslah.com", demoUserPassword,
                Role.USER, "Demo", "User");
        seedUser("organizer1", "organizer1@waslah.com", demoUserPassword,
                Role.USER, "Organizer", "One");
        seedUser("organizer2", "organizer2@waslah.com", demoUserPassword,
                Role.USER, "Organizer", "Two");
        for (int i = 1; i <= 4; i++) {
            seedUser("student" + i, "student" + i + "@waslah.com",
                    demoUserPassword, Role.USER, "Student", String.valueOf(i));
        }

        User cafeManager = seedUser(
                "cafe.manager",
                "cafe.manager@waslah.com",
                cafeManagerPassword,
                Role.CAFE_MANAGER,
                "Cafe",
                "Manager"
        );

        if (cafeManager == null) {
            log.warn("Cafe seed data skipped: configure app.seed.cafe-manager-password.");
            return;
        }

        Cafe manamaCafe = seedCafe(
                cafeManager,
                "Bahrain Study Cafe",
                "A quiet study cafe with comfortable spaces for individual and group work.",
                "Manama, Bahrain"
        );

        Cafe seefCafe = seedCafe(
                cafeManager,
                "WASLAH Workspace Cafe",
                "A flexible workspace for students, freelancers, and professionals.",
                "Seef, Bahrain"
        );

        seedStudySpace(manamaCafe, "Quiet Desk 1", 1, 2.0);
        seedStudySpace(manamaCafe, "Quiet Desk 2", 1, 2.0);
        seedStudySpace(manamaCafe, "Group Room", 6, 8.0);
        seedStudySpace(seefCafe, "Individual Desk", 1, 3.0);
        seedStudySpace(seefCafe, "Meeting Room", 8, 10.0);

        log.info("WASLAH seed-data initialization completed.");
    }

    /**
     * Creates the administrator account once, using configured credentials.
     * The password must be at least eight characters and is never hardcoded
     * in this class.
     */
    private void seedAdmin() {
        if (adminPassword == null || adminPassword.length() < 8) {
            log.warn("Admin not seeded: set ADMIN_PASSWORD to at least 8 characters.");
            return;
        }

        if (userRepository.existsByUsername(adminUsername)
                || userRepository.existsByEmail(adminEmail)) {
            log.info("Admin seed skipped because the username or email already exists.");
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

    /**
     * Finds a seed user by username or email, or creates it if missing.
     * Existing accounts are not overwritten or assigned a new password.
     *
     * @param username unique username for the seed account
     * @param email unique email for the seed account
     * @param rawPassword configured password for a new account
     * @param role role assigned to a newly created account
     * @param firstName profile first name
     * @param lastName profile last name
     * @return the existing or newly created user, or {@code null} if the
     * password is not configured or an email already belongs to another user
     */
    private User seedUser(
            String username,
            String email,
            String rawPassword,
            Role role,
            String firstName,
            String lastName
    ) {
        User existing = userRepository.findByUsername(username).orElse(null);
        if (existing != null) {
            return existing;
        }

        existing = userRepository.findByEmail(email).orElse(null);
        if (existing != null) {
            log.warn("Seed user '{}' skipped because its email already belongs to another account.", username);
            return null;
        }

        if (rawPassword == null || rawPassword.length() < 8) {
            log.warn("Seed user '{}' skipped: configure a password of at least 8 characters.", username);
            return null;
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);

        UserProfile profile = new UserProfile();
        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setBio("WASLAH demonstration account");
        profile.setLocation("Bahrain");
        user.setUserProfile(profile);

        User savedUser = userRepository.save(user);
        log.info("Seed user '{}' created with role {}.", username, role);
        return savedUser;
    }

    /**
     * Finds a cafe belonging to the specified manager or creates it.
     *
     * @param manager user who manages the cafe
     * @param name cafe name used to identify the seed record
     * @param description cafe description
     * @param location cafe location
     * @return the existing or newly created cafe
     */
    private Cafe seedCafe(User manager, String name, String description, String location) {
        List<Cafe> managedCafes = cafeRepository.findByManager(manager);

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

    /**
     * Finds a study space by its cafe and name, or creates it.
     *
     * @param cafe cafe that owns the study space
     * @param name study-space name
     * @param capacity maximum number of people
     * @param pricePerHour hourly price in the application's configured currency
     * @return the existing or newly created study space
     */
    private StudySpace seedStudySpace(Cafe cafe, String name, int capacity, double pricePerHour) {
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

        StudySpace savedSpace = studySpaceRepository.save(space);
        log.info("Seed study space '{}' created for cafe '{}'.", name, cafe.getName());
        return savedSpace;
    }

}
