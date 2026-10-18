package com.ga.waslah.service;

import com.ga.waslah.model.EmailVerificationToken;
import com.ga.waslah.model.User;
import com.ga.waslah.repository.EmailVerificationTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ga.waslah.exception.BadRequestException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;


    @Test
    void shouldVerifyEmailWithValidToken() {

        // Arrange
        User user = new User();
        user.setEmail("noor@example.com");
        user.setEmailVerified(false);

        EmailVerificationToken verificationToken =
                new EmailVerificationToken();

        verificationToken.setToken("valid-token");
        verificationToken.setUser(user);
        verificationToken.setExpiresAt(
                LocalDateTime.now().plusHours(24)
        );

        when(tokenRepository.findByToken("valid-token"))
                .thenReturn(Optional.of(verificationToken));

        // Act
        emailVerificationService.verifyEmail("valid-token");

        // Assert
        assertTrue(user.isEmailVerified());

        verify(tokenRepository).delete(verificationToken);
    }
    @Test
    void shouldRejectExpiredVerificationToken() {

        // Arrange
        User user = new User();
        user.setEmail("noor@example.com");
        user.setEmailVerified(false);

        EmailVerificationToken verificationToken =
                new EmailVerificationToken();

        verificationToken.setToken("expired-token");
        verificationToken.setUser(user);

        verificationToken.setExpiresAt(
                LocalDateTime.now().minusHours(1)
        );

        when(tokenRepository.findByToken("expired-token"))
                .thenReturn(Optional.of(verificationToken));

        // Act & Assert
        assertThrows(
                BadRequestException.class,
                () -> emailVerificationService.verifyEmail("expired-token")
        );

        // Email should remain unverified
        assertFalse(user.isEmailVerified());

        // Expired token should be deleted
        verify(tokenRepository).delete(verificationToken);
    }
}