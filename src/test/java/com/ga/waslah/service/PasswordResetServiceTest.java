package com.ga.waslah.service;

import com.ga.waslah.exception.BadRequestException;
import com.ga.waslah.model.PasswordResetToken;
import com.ga.waslah.model.User;
import com.ga.waslah.repository.PasswordResetTokenRepository;
import com.ga.waslah.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private PasswordResetToken token(LocalDateTime expiry, boolean used) {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("abc");
        token.setUser(new User());
        token.setExpiryDate(expiry);
        token.setUsed(used);
        return token;
    }

    @Test
    void shouldAcceptValidToken() {
        when(tokenRepository.findByToken("abc")).thenReturn(
                Optional.of(token(LocalDateTime.now().plusMinutes(10), false)));

        assertDoesNotThrow(() -> passwordResetService.validateToken("abc"));
    }

    @Test
    void shouldRejectExpiredToken() {
        when(tokenRepository.findByToken("abc")).thenReturn(
                Optional.of(token(LocalDateTime.now().minusMinutes(1), false)));

        assertThrows(BadRequestException.class,
                () -> passwordResetService.validateToken("abc"));
    }

    @Test
    void shouldRejectUsedToken() {
        when(tokenRepository.findByToken("abc")).thenReturn(
                Optional.of(token(LocalDateTime.now().plusMinutes(10), true)));

        assertThrows(BadRequestException.class,
                () -> passwordResetService.validateToken("abc"));
    }

    @Test
    void shouldResetPasswordAndMarkTokenUsed() {
        PasswordResetToken resetToken =
                token(LocalDateTime.now().plusMinutes(10), false);
        when(tokenRepository.findByToken("abc")).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("NewPassw0rd!")).thenReturn("hashed");

        passwordResetService.resetPassword("abc", "NewPassw0rd!");

        assertEquals("hashed", resetToken.getUser().getPassword());
        assertTrue(resetToken.isUsed());
        verify(tokenRepository).save(resetToken);
    }
}
