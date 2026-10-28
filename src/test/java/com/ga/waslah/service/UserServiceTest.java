package com.ga.waslah.service;

import com.ga.waslah.exception.ConflictException;
import com.ga.waslah.exception.ResourceNotFoundException;
import com.ga.waslah.model.User;
import com.ga.waslah.model.UserStatus;
import com.ga.waslah.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailVerificationService emailVerificationService;
    @Mock private AuditService auditService;

    @InjectMocks
    private UserService userService;

    private User sara;

    @BeforeEach
    void setUp() {
        sara = new User();
        sara.setId(2L);
        sara.setUsername("sara");
        sara.setStatus(UserStatus.ACTIVE);
    }

    @Test
    void adminCanDeactivateAnotherUser() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(sara));

        userService.deactivateUser(2L, "admin");

        assertEquals(UserStatus.INACTIVE, sara.getStatus());
        verify(auditService).log("admin", "USER_DEACTIVATED", "USER", 2L);
    }

    @Test
    void adminCannotDeactivateTheirOwnAccount() {
        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setStatus(UserStatus.ACTIVE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(ConflictException.class, () -> userService.deactivateUser(1L, "admin"));

        assertEquals(UserStatus.ACTIVE, admin.getStatus());
        verify(userRepository, never()).save(any());
    }

    @Test
    void reactivationRestoresAccessAndClearsTheLoginLock() {
        sara.setStatus(UserStatus.INACTIVE);
        sara.setFailedLoginAttempts(3);
        sara.setLockedUntil(LocalDateTime.now().plusMinutes(1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(sara));

        userService.reactivateUser(2L, "admin");

        assertEquals(UserStatus.ACTIVE, sara.getStatus());
        assertEquals(0, sara.getFailedLoginAttempts());
        assertNull(sara.getLockedUntil());
        verify(auditService).log("admin", "USER_REACTIVATED", "USER", 2L);
    }

    @Test
    void reactivatingAnActiveUserIsAConflict() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(sara));

        assertThrows(ConflictException.class, () -> userService.reactivateUser(2L, "admin"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void reactivatingAnUnknownUserIsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.reactivateUser(99L, "admin"));
    }
}