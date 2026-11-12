
package com.ga.identity_service.service;

import com.ga.identity_service.exception.BadRequestException;
import com.ga.identity_service.model.PasswordResetToken;
import com.ga.identity_service.model.User;
import com.ga.identity_service.repository.PasswordResetTokenRepository;
import com.ga.identity_service.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.base-url:http://localhost:8081}")
    private String baseUrl;

    @Transactional
    public void createAndSendResetToken(String email) {

        // Do not disclose whether an account exists.
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return;
        }

        tokenRepository.deleteByUser(user);
        tokenRepository.flush();

        PasswordResetToken token = new PasswordResetToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(15));
        token.setUsed(false);

        tokenRepository.save(token);

        String link = baseUrl + "/auth/reset-password?token="
                + token.getToken();

        Runnable sendEmail = () ->
                emailService.sendPasswordResetEmail(
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

    @Transactional(readOnly = true)
    public void validateToken(String tokenValue) {
        getValidToken(tokenValue);
    }

    @Transactional
    public void resetPassword(
            String tokenValue,
            String newPassword) {

        PasswordResetToken token = getValidToken(tokenValue);
        User user = token.getUser();

        user.setPassword(passwordEncoder.encode(newPassword));

        // Mark token as used so it cannot be reused.
        token.setUsed(true);

        userRepository.save(user);
        tokenRepository.save(token);
    }

    private PasswordResetToken getValidToken(String tokenValue) {

        PasswordResetToken token =
                tokenRepository.findByToken(tokenValue)
                        .orElseThrow(() ->
                                new BadRequestException("Invalid password reset token"));

        if (!token.getExpiryDate().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Password reset token has expired");
        }

        if (token.isUsed()) {
            throw new BadRequestException("Password reset token has already been used");
        }

        return token;
    }
}
