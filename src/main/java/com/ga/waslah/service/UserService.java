package com.ga.waslah.service;

import com.ga.waslah.dto.ChangePasswordRequest;
import com.ga.waslah.dto.RegisterRequest;
import com.ga.waslah.exception.BadRequestException;
import com.ga.waslah.exception.ConflictException;
import com.ga.waslah.exception.ResourceNotFoundException;
import com.ga.waslah.model.Role;
import com.ga.waslah.model.User;
import com.ga.waslah.model.UserStatus;
import com.ga.waslah.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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

    /**
     * Registers a new user and sends an email verification message.
     *
     * @param request registration details provided by the user
     * @return the saved user
     * @throws ConflictException if the username or email already exists
     */
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

        // Store a hash of the password, never the raw password.
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        // Every new account starts with the standard USER role.
        user.setRole(Role.USER);

        // New accounts are active but cannot log in until email verification.
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);

        // Generate a verification token and send the verification email.
        emailVerificationService.createAndSendVerificationToken(savedUser);

        log.info("User registered successfully with ID {}", savedUser.getId());

        return savedUser;
    }

    /**
     * Deactivates a user account and logs who performed the action.
     *
     * @param userId ID of the account to deactivate
     * @param performedBy username of the authenticated administrator
     * @throws ResourceNotFoundException if the target user does not exist
     * @throws ConflictException if the account is already inactive
     */
    public void deactivateUser(Long userId, String performedBy) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User with ID " + userId + " was not found"
                        )
                );

        if (user.getStatus() == UserStatus.INACTIVE) {
            log.warn(
                    "Admin {} could not deactivate user {} because the account is already inactive",
                    performedBy,
                    userId
            );

            throw new ConflictException("User is already inactive");
        }

        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        auditService.log(
                performedBy,
                "USER_DEACTIVATED",
                "USER",
                user.getId()
        );

        // Record both the administrator and the account affected.
        log.info(
                "Admin {} deactivated user account {}",
                performedBy,
                userId
        );
    }


    /**
     * Changes a user's password after validating the current password.
     *
     * @param username username of the account
     * @param request current and new password details
     * @throws ResourceNotFoundException if the user does not exist
     * @throws BadRequestException if the current password is incorrect
     * or the new password matches the current password
     */
    public void changePassword(
            String username,
            ChangePasswordRequest request
    ) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        // Verify the current password before allowing the change.
        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPassword()
        )) {
            log.warn("Password change rejected due to an incorrect current password");
            throw new BadRequestException("Current password is incorrect");
        }

        // Prevent the user from reusing the current password.
        if (passwordEncoder.matches(
                request.getNewPassword(),
                user.getPassword()
        )) {
            log.warn("Password change rejected because the new password matches the current password");
            throw new BadRequestException(
                    "New password must be different from the current password"
            );
        }

        // Encode the new password before storing it.
        user.setPassword(
                passwordEncoder.encode(request.getNewPassword())
        );

        userRepository.save(user);

        log.info("Password changed successfully for user ID {}", user.getId());
    }

    /**
     * Checks whether a user's login lock is still active.
     * If the lock has expired, clears the lock and resets failed attempts.
     *
     * @param user user whose login status is being checked
     * @return true if the user is currently locked; otherwise false
     */
    public boolean isLoginLocked(User user) {

        if (user.getLockedUntil() == null) {
            return false;
        }

        if (user.getLockedUntil().isAfter(LocalDateTime.now())) {
            return true;
        }

        // The lock has expired, so allow new login attempts.
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        log.info("Expired login lock cleared for user ID {}", user.getId());

        return false;
    }

    /**
     * Records a failed login attempt and locks the account after three failures.
     *
     * @param user user whose login attempt failed
     */
    public void recordFailedLogin(User user) {

        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        // Lock the account for one minute after three failed attempts.
        if (attempts >= 3) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(1));

            log.warn(
                    "Login locked for user ID {} after {} failed attempts",
                    user.getId(),
                    attempts
            );
        } else {
            log.debug(
                    "Failed login attempt {} recorded for user ID {}",
                    attempts,
                    user.getId()
            );
        }

        userRepository.save(user);
    }

    /**
     * Clears the failed-login counter and any active login lock.
     * Typically called after a successful login.
     *
     * @param user user whose login tracking will be reset
     */
    public void resetLoginAttempts(User user) {

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        userRepository.save(user);

        log.debug("Login attempt tracking reset for user ID {}", user.getId());
    }
}