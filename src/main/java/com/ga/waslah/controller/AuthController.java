package com.ga.waslah.controller;

import com.ga.waslah.dto.ForgotPasswordRequest;
import com.ga.waslah.dto.LoginRequest;
import com.ga.waslah.dto.LoginResponse;
import com.ga.waslah.dto.ResetPasswordRequest;
import com.ga.waslah.exception.BadRequestException;
import com.ga.waslah.exception.ForbiddenException;
import com.ga.waslah.model.User;
import com.ga.waslah.model.UserStatus;
import com.ga.waslah.repository.UserRepository;
import com.ga.waslah.security.JwtService;
import com.ga.waslah.service.EmailVerificationService;
import com.ga.waslah.service.PasswordResetService;
import com.ga.waslah.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

@Slf4j
@Tag(name = "Authentication", description = "Login, email verification and password recovery (public endpoints, rate limited).")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final UserService userService;
    private final PasswordResetService passwordResetService;

    @Operation(
            summary = "Log in and receive a JWT",
            description = "Authenticates a verified, active account and returns a 24-hour JWT. An unknown username returns 400; a wrong password returns 401; unverified, inactive or temporarily locked accounts return 403. Three wrong passwords lock the account for 1 minute. Limited to 5 requests per minute per IP."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful, JWT returned"),
            @ApiResponse(responseCode = "400", description = "Unknown username"),
            @ApiResponse(responseCode = "401", description = "Wrong password"),
            @ApiResponse(responseCode = "403", description = "Email not verified, account inactive or temporarily locked"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @SecurityRequirements
    @PostMapping("/login")
    public LoginResponse login(
            @RequestBody LoginRequest loginRequest
    ) {

        User user = userRepository.findByUsername(
                loginRequest.getUsername()
        ).orElseThrow(() -> {
            log.warn("Login failed: username not found");
            return new BadRequestException(
                    "Invalid username or password"
            );
        });

        // ACCOUNT STATUS
        if (user.getStatus() == UserStatus.INACTIVE) {
            log.warn("Login denied: account is inactive");

            throw new ForbiddenException(
                    "Your account is inactive and cannot be used"
            );
        }

        // EMAIL VERIFICATION
        if (!user.isEmailVerified()) {
            log.warn("Login denied: email is not verified");

            throw new ForbiddenException(
                    "Please verify your email before logging in"
            );
        }

        // TEMPORARY LOGIN LOCK
        if (userService.isLoginLocked(user)) {
            log.warn("Login denied: account is temporarily locked");

            throw new ForbiddenException(
                    "Too many failed login attempts. Please try again later."
            );
        }

        // AUTHENTICATION
        try {

            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    loginRequest.getUsername(),
                                    loginRequest.getPassword()
                            )
                    );

            // Password is correct.
            userService.resetLoginAttempts(user);

            log.info("Login successful");

            String token = jwtService.generateToken(
                    authentication.getName()
            );

            return new LoginResponse(token);

        } catch (BadCredentialsException exception) {

            log.warn("Login failed: invalid credentials");

            // Password is incorrect.
            userService.recordFailedLogin(user);

            // User reached 3 failed attempts.
            if (user.getFailedLoginAttempts() >= 3) {
                log.warn("Login denied: account reached failed-attempt limit");

                throw new ForbiddenException(
                        "Too many failed login attempts. Your account is locked for 1 minute."
                );
            }

            throw new BadCredentialsException(
                    "Invalid username or password"
            );
        }
    }

    @Operation(
            summary = "Verify an email address",
            description = "Confirms the account that owns the token (token is sent by email after registration and expires after 24 hours)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email verified"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token"),
            @ApiResponse(responseCode = "409", description = "Email already verified")
    })
    @SecurityRequirements
    @GetMapping("/verify-email")
    public String verifyEmail(
            @RequestParam String token
    ) {

        emailVerificationService.verifyEmail(token);

        log.info("Email verification completed");

        return "Email verified successfully. You can now login.";
    }

    @Operation(
            summary = "Request a password-reset email",
            description = "Creates a single-use reset token (valid 15 minutes) and emails it to the address. Limited to 3 requests per minute per IP."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reset email sent"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid request"),
            @ApiResponse(responseCode = "404", description = "Resource not found"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @SecurityRequirements
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {

        passwordResetService.createAndSendResetToken(
                request.email()
        );

        log.info("Password reset request processed");

        return ResponseEntity.ok(
                "Password reset email sent successfully"
        );
    }

    @Operation(
            summary = "Open the password-reset link from the email",
            description = "The link in the reset email lands here. Confirms the token is valid, unused and unexpired, and explains the next step: send it to POST /auth/reset-password together with the new password. Counts toward the reset rate limit (3 per minute per IP)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token is valid"),
            @ApiResponse(responseCode = "400", description = "Invalid, expired or already used token"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @SecurityRequirements
    @GetMapping("/reset-password")
    public ResponseEntity<String> checkResetLink(
            @RequestParam String token
    ) {

        passwordResetService.validateToken(token);

        return ResponseEntity.ok(
                "This reset link is valid. Send POST /auth/reset-password "
                        + "with {\"token\": \"" + token
                        + "\", \"newPassword\": \"<your new password>\"}."
        );
    }

    @Operation(
            summary = "Reset a password using a reset token",
            description = "Sets a new password when given a valid, unused, unexpired token. Limited to 3 requests per minute per IP."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset"),
            @ApiResponse(responseCode = "400", description = "Validation error, or invalid/expired/used token"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @SecurityRequirements
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {

        passwordResetService.resetPassword(
                request.token(),
                request.newPassword()
        );

        log.info("Password reset completed");

        return ResponseEntity.ok(
                "Password has been reset successfully"
        );
    }
}
