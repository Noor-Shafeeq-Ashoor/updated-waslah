package com.ga.waslah.service;

import com.ga.waslah.exception.BadRequestException;
import com.ga.waslah.exception.ConflictException;
import com.ga.waslah.exception.ResourceNotFoundException;
import com.ga.waslah.model.EmailVerificationToken;
import com.ga.waslah.model.User;
import com.ga.waslah.repository.EmailVerificationTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;

@Service
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;

    // Public address of this API, used to build links in emails.
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl = "http://localhost:8080";

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            EmailService emailService
    ) {
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    public void createAndSendVerificationToken(User user) {

        // Remove any existing token for this user.
        tokenRepository.deleteByUser(user);

        // Generate a unique verification token.
        String tokenValue = UUID.randomUUID().toString();

        EmailVerificationToken verificationToken =
                new EmailVerificationToken();

        verificationToken.setToken(tokenValue);
        verificationToken.setUser(user);
        verificationToken.setExpiresAt(
                LocalDateTime.now().plusHours(24)
        );

        tokenRepository.save(verificationToken);

        String verificationLink =
                baseUrl + "/auth/verify-email?token="
                        + tokenValue;

        emailService.sendVerificationEmail(
                user.getEmail(),
                verificationLink
        );
    }

    @Transactional
    public void verifyEmail(String tokenValue) {

        EmailVerificationToken verificationToken =
                tokenRepository.findByToken(tokenValue)
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Invalid verification token"
                                )
                        );

        if (verificationToken.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            tokenRepository.delete(verificationToken);

            throw new BadRequestException(
                    "Verification token has expired"
            );
        }

        User user = verificationToken.getUser();

        if (user.isEmailVerified()) {
            throw new ConflictException(
                    "Email is already verified"
            );
        }

        user.setEmailVerified(true);

        tokenRepository.delete(verificationToken);
    }
}
