package com.ga.waslah.service;

import com.ga.waslah.exception.BadRequestException;
import com.ga.waslah.exception.ResourceNotFoundException;
import com.ga.waslah.model.PasswordResetToken;
import com.ga.waslah.model.User;
import com.ga.waslah.repository.PasswordResetTokenRepository;
import com.ga.waslah.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;

@Service
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // Public address of this API, used to build links in emails.
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl = "http://localhost:8080";

    public PasswordResetService(
            PasswordResetTokenRepository tokenRepository,
            UserRepository userRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    public void createAndSendResetToken(String email) {

        // Do not reveal whether the address belongs to an account: an unknown
        // address is silently ignored and the caller gets the same answer.
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return;
        }

        // Remove any previous reset token.
        tokenRepository.deleteByUser(user);

        // Generate a new reset token.
        String tokenValue = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();

        resetToken.setToken(tokenValue);
        resetToken.setUser(user);
        resetToken.setExpiryDate(
                LocalDateTime.now().plusMinutes(15)
        );
        resetToken.setUsed(false);

        tokenRepository.save(resetToken);

        String resetLink =
                baseUrl + "/auth/reset-password?token="
                        + tokenValue;

        emailService.sendPasswordResetEmail(
                user.getEmail(),
                resetLink
        );
    }

    /**
     * Checks that a reset token exists, is unused and has not expired.
     * Used when the user opens the link from the email.*/
    public void validateToken(String tokenValue) {

        PasswordResetToken resetToken =
                tokenRepository.findByToken(tokenValue)
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Invalid password reset token"
                                )
                        );

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException(
                    "Password reset token has expired"
            );
        }

        if (resetToken.isUsed()) {
            throw new BadRequestException(
                    "Password reset token has already been used"
            );
        }
    }

    @Transactional
    public void resetPassword(
            String tokenValue,
            String newPassword
    ) {

        PasswordResetToken resetToken =
                tokenRepository.findByToken(tokenValue)
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Invalid password reset token"
                                )
                        );

        if (resetToken.getExpiryDate()
                .isBefore(LocalDateTime.now())) {

            tokenRepository.delete(resetToken);

            throw new BadRequestException(
                    "Password reset token has expired"
            );
        }

        if (resetToken.isUsed()) {
            throw new BadRequestException(
                    "Password reset token has already been used"
            );
        }

        User user = resetToken.getUser();

        user.setPassword(
                passwordEncoder.encode(newPassword)
        );

        resetToken.setUsed(true);

        userRepository.save(user);
        tokenRepository.save(resetToken);
    }
}