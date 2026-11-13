package com.ga.identity_service.service;

import com.ga.identity_service.dto.*;
import com.ga.identity_service.exception.*;
import com.ga.identity_service.model.*;
import com.ga.identity_service.repository.UserRepository;

import com.ga.identity_service.util.PagingUtil;
import com.ga.identity_service.util.Specs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Handles user registration, account status, password changes,
 * and failed-login tracking.
 */
@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final AuditService auditService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailVerificationService emailVerificationService,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
        this.auditService = auditService;
    }


     // Registers a new user and sends an email verification message.

    @Transactional
    public User register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration rejected because the username already exists");
            throw new ConflictException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration rejected because the email already exists");
            throw new ConflictException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(false);
        user.setFailedLoginAttempts(0);

        User savedUser = userRepository.save(user);
        emailVerificationService.createAndSendVerificationToken(savedUser);

        log.info("User registered successfully with ID {}", savedUser.getId());
        return savedUser;
    }

    //Deactivates a user account and logs who performed the action.


    @Transactional
    public void deactivateUser(Long userId, String performedBy) {
        User user = findUser(userId);

        if (user.getUsername().equals(performedBy)) {
            throw new ConflictException("You cannot deactivate your own account");
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new ConflictException("User is already inactive");
        }

        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        auditService.log(performedBy, "USER_DEACTIVATED", "USER", userId);
    }

    //Re-enables a deactivated account and clears any login lock.
    @Transactional
    public void reactivateUser(Long userId, String performedBy) {

        User user = findUser(userId);

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new ConflictException("User is already active");
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        userRepository.save(user);

        auditService.log(performedBy, "USER_REACTIVATED", "USER", user.getId());

        log.info("Admin {} deactivated user account {}", performedBy, userId);
    }


    // Lists users for the admin dashboard with optional filters.

    @Transactional(readOnly = true)

    public PageResponse<AdminUserSummary> listUsers(
            Role role,
            UserStatus status,
            String keyword,
            int page,
            int size,
            String sort,
            String direction
    ) {
        Specification<User> specification =
                Specs.<User>equal(role, "role")
                        .and(Specs.equal(status, "status"))
                        .and(
                                Specs.<User>containsIgnoreCase(keyword, "username")
                                        .or(Specs.containsIgnoreCase(keyword, "email"))
                        );

        Pageable pageable = PagingUtil.createPageable(
                page, size, sort, direction, "id",
                Set.of("id", "username", "email", "role", "status")
        );

        Page<User> result = userRepository.findAll(specification, pageable);

        return new PageResponse<>(
                result.getContent().stream().map(AdminUserSummary::from).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }

    // Changes a user's password after validating the current password.

    public void changePassword(String username, ChangePasswordRequest request) {

        User user = userRepository.findByUsername(username).orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        // Verify the current password before allowing the change.
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Password change rejected due to an incorrect current password");
            throw new BadRequestException("Current password is incorrect");
        }
        // Prevent the user from reusing the current password.
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            log.warn("Password change rejected because the new password matches the current password");
            throw new BadRequestException("New password must be different from the current password");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);

        log.info("Password changed successfully for user ID {}", user.getId());
    }

    // Checks whether a user's login lock is still active.

    public boolean isLoginLocked(User user) {
        if (user.getLockedUntil() == null) {
            return false;
        }

        if (user.getLockedUntil().isAfter(LocalDateTime.now())) {
            return true;
        }

        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        log.info("Expired login lock cleared for user ID {}", user.getId());

        return false;
    }

    // Lock the account after three failed attempts.
    public void recordFailedLogin(User user) {

        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        // Lock the account for one minute after three failed attempts.
        if (attempts >= 3) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(1));

            log.warn("Login locked for user ID {} after {} failed attempts", user.getId(), attempts);
        }
        else {
            log.debug("Failed login attempt {} recorded for user ID {}", attempts, user.getId());
        }

        userRepository.save(user);
    }

    // Reset login counters after successful authentication.
    public void resetLoginAttempts(User user) {

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        log.debug("Login attempt tracking reset for user ID {}", user.getId());
    }
    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() ->
                        new ResourceNotFoundException("User with ID " + id + " was not found"));
    }

}