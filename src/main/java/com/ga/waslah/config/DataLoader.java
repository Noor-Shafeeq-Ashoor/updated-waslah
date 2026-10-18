package com.ga.waslah.config;

import com.ga.waslah.model.*;
import com.ga.waslah.repository.CafeRepository;
import com.ga.waslah.repository.StudySpaceRepository;
import com.ga.waslah.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CafeRepository cafeRepository;
    private final StudySpaceRepository studySpaceRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminEmail;
    private final String adminPassword;

    public DataLoader(
            UserRepository userRepository,
            CafeRepository cafeRepository,
            StudySpaceRepository studySpaceRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.username:admin}") String adminUsername,
            @Value("${app.admin.email:admin@waslah.com}") String adminEmail,
            @Value("${app.admin.password:}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.cafeRepository = cafeRepository;
        this.studySpaceRepository = studySpaceRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {

        // =========================
        // CREATE ADMIN (from environment)
        // =========================

        seedAdmin();

        // =========================
        // CREATE CAFE MANAGER
        // =========================

        User manager = userRepository
                .findByUsername("cafe.manager")
                .orElseGet(() -> {

                    User newManager = new User();

                    newManager.setUsername("cafe.manager");
                    newManager.setEmail("cafe.manager@waslah.com");
                    newManager.setPassword(
                            passwordEncoder.encode("Cafe@12345")
                    );
                    newManager.setRole(Role.CAFE_MANAGER);
                    newManager.setStatus(UserStatus.ACTIVE);
                    newManager.setEmailVerified(true);

                    return userRepository.save(newManager);
                });

        // =========================
        // CREATE FIRST CAFE
        // =========================

        Cafe firstCafe;

        if (cafeRepository.findByManager(manager).isEmpty()) {

            firstCafe = new Cafe();

            firstCafe.setName("Bahrain Study Cafe");
            firstCafe.setDescription(
                    "A quiet study cafe with comfortable spaces for individual and group work."
            );
            firstCafe.setLocation("Manama, Bahrain");
            firstCafe.setActive(true);
            firstCafe.setManager(manager);

            firstCafe = cafeRepository.save(firstCafe);

            // Quiet Desk 1
            StudySpace space1 = new StudySpace();
            space1.setName("Quiet Desk 1");
            space1.setCapacity(1);
            space1.setPricePerHour(2.0);
            space1.setAvailable(true);
            space1.setCafe(firstCafe);

            // Quiet Desk 2
            StudySpace space2 = new StudySpace();
            space2.setName("Quiet Desk 2");
            space2.setCapacity(1);
            space2.setPricePerHour(2.0);
            space2.setAvailable(true);
            space2.setCafe(firstCafe);

            // Group Room
            StudySpace space3 = new StudySpace();
            space3.setName("Group Room");
            space3.setCapacity(6);
            space3.setPricePerHour(8.0);
            space3.setAvailable(true);
            space3.setCafe(firstCafe);

            studySpaceRepository.save(space1);
            studySpaceRepository.save(space2);
            studySpaceRepository.save(space3);
        }

        // =========================
        // CREATE SECOND CAFE
        // =========================

        if (cafeRepository.findByManager(manager).size() < 2) {

            Cafe secondCafe = new Cafe();

            secondCafe.setName("WASLAH Workspace Cafe");
            secondCafe.setDescription(
                    "A flexible workspace for students, freelancers, and professionals."
            );
            secondCafe.setLocation("Seef, Bahrain");
            secondCafe.setActive(true);
            secondCafe.setManager(manager);

            secondCafe = cafeRepository.save(secondCafe);

            // Individual Desk
            StudySpace space4 = new StudySpace();
            space4.setName("Individual Desk");
            space4.setCapacity(1);
            space4.setPricePerHour(3.0);
            space4.setAvailable(true);
            space4.setCafe(secondCafe);

            // Meeting Room
            StudySpace space5 = new StudySpace();
            space5.setName("Meeting Room");
            space5.setCapacity(8);
            space5.setPricePerHour(10.0);
            space5.setAvailable(true);
            space5.setCafe(secondCafe);

            studySpaceRepository.save(space4);
            studySpaceRepository.save(space5);
        }
    }

    /**
     * Creates the administrator account once, using ADMIN_USERNAME,
     * ADMIN_EMAIL and ADMIN_PASSWORD. Nothing is created when
     * ADMIN_PASSWORD is not set or is shorter than 8 characters,
     * so no default admin password exists in the source code.
     */
    private void seedAdmin() {

        if (adminPassword == null || adminPassword.length() < 8) {
            log.warn("No admin created: set ADMIN_PASSWORD "
                    + "(at least 8 characters) to seed an admin account");
            return;
        }

        if (userRepository.existsByUsername(adminUsername)
                || userRepository.existsByEmail(adminEmail)) {
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

        log.info("Admin account '{}' created", adminUsername);
    }
}
