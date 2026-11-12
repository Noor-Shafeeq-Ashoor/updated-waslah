package com.ga.identity_service.service;

import com.ga.identity_service.exception.BadRequestException;
import com.ga.identity_service.exception.ConflictException;
import com.ga.identity_service.model.EmailVerificationToken;
import com.ga.identity_service.model.User;
import com.ga.identity_service.repository.EmailVerificationTokenRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.UUID;


@Service
//@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;

    public EmailVerificationService(EmailVerificationTokenRepository tokenRepository, EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    @Value("${app.base-url:http://localhost:8081}")
    private String baseUrl;

    @Transactional
    public void createAndSendVerificationToken(User user) {

        tokenRepository.deleteByUser(user);
        tokenRepository.flush();

        String tokenValue = UUID.randomUUID().toString();

        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken(tokenValue);
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusHours(24));

        tokenRepository.save(token);

        String link = baseUrl + "/auth/verify-email?token="
                + tokenValue;

        Runnable sendEmail = () ->
                emailService.sendVerificationEmail(
                        user.getEmail(), link);

        if (TransactionSynchronizationManager
                .isSynchronizationActive()
                && TransactionSynchronizationManager
                .isActualTransactionActive()) {

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            sendEmail.run();
                        }
                    });
        } else {
            sendEmail.run();
        }
    }

    @Transactional
    public void verifyEmail(String tokenValue) {

        EmailVerificationToken token =
                tokenRepository.findByToken(tokenValue)
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Invalid verification token"));

        if (!token.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BadRequestException(
                    "Verification token has expired");
        }

        User user = token.getUser();

        if (user.isEmailVerified()) {
            throw new ConflictException(
                    "Email is already verified");
        }

        user.setEmailVerified(true);
        tokenRepository.delete(token);
    }
}