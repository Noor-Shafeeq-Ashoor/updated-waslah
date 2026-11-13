package com.ga.identity_service.config;

import com.ga.identity_service.model.Role;
import com.ga.identity_service.model.User;
import com.ga.identity_service.model.UserStatus;
import com.ga.identity_service.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@Profile("!test")
public class
AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminEmail;
    private final String adminPassword;

    public AdminSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.username:admin}") String adminUsername,
            @Value("${app.admin.email:admin@waslah.com}") String adminEmail,
            @Value("${app.admin.password:}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {

        if (adminPassword == null || adminPassword.length() < 8) {
            log.warn("Admin not seeded: set ADMIN_PASSWORD to at least 8 characters.");
            return;
        }

        if (userRepository.existsByUsername(adminUsername)
                || userRepository.existsByEmail(adminEmail)) {
            log.info("Admin seed skipped because username or email already exists.");
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
}