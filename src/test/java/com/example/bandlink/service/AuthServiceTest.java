package com.example.bandlink.service;

import com.example.bandlink.dto.RegisterRequest;
import com.example.bandlink.entity.User;
import com.example.bandlink.entity.EmailVerificationToken;
import com.example.bandlink.entity.PasswordResetToken;
import com.example.bandlink.repository.EmailVerificationTokenRepository;
import com.example.bandlink.repository.PasswordResetTokenRepository;
import com.example.bandlink.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock EmailVerificationTokenRepository verificationTokens;
    @Mock PasswordResetTokenRepository resetTokens;
    @Mock MailService mail;
    @Mock AccountDeletionService accountDeletion;
    @InjectMocks AuthService authService;

    @Test
    void registerNormalizesEmailAndNeverStoresRawPassword() {
        when(userRepository.existsByEmail("a@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.register(new RegisterRequest(" Haruki ", " A@EXAMPLE.COM ", "password123"));

        assertEquals("a@example.com", response.email());
        assertFalse(response.emailVerified());
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(argThat(user -> user.getPasswordHash().equals("bcrypt-hash") && user.getUsername().equals("Haruki")));
        verify(mail).sendVerification(eq("a@example.com"), anyString());
    }

    @Test
    void duplicateEmailIsRejectedBeforeEncoding() {
        when(userRepository.existsByEmail("a@example.com")).thenReturn(true);

        assertThrows(AuthService.EmailAlreadyUsedException.class,
                () -> authService.register(new RegisterRequest("Haruki", "a@example.com", "password123")));
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyEmailMarksUserAndConsumesToken() {
        User user = new User("Haruki", "a@example.com", "hash");
        EmailVerificationToken token = new EmailVerificationToken(user, "verify-token", LocalDateTime.now().plusHours(1));
        when(verificationTokens.findByToken("verify-token")).thenReturn(Optional.of(token));

        authService.verifyEmail(" verify-token ");

        assertTrue(user.isEmailVerified());
        assertNotNull(token.getUsedAt());
    }

    @Test
    void resendVerificationInvalidatesOldLinksAndSendsFreshLink() {
        User user = new User("Haruki", "a@example.com", "hash");
        EmailVerificationToken oldToken = new EmailVerificationToken(user, "old-token", LocalDateTime.now().plusHours(1));
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(verificationTokens.findAllByUserIdAndUsedAtIsNull(42L)).thenReturn(List.of(oldToken));

        authService.resendVerification(42L);

        assertNotNull(oldToken.getUsedAt());
        verify(verificationTokens).save(argThat(token -> token.getUser() == user && token.getExpiresAt().isAfter(LocalDateTime.now())));
        verify(mail).sendVerification(eq("a@example.com"), anyString());
    }

    @Test
    void expiredPasswordResetTokenIsRejected() {
        User user = new User("Haruki", "a@example.com", "old-hash");
        PasswordResetToken token = new PasswordResetToken(user, "reset-token", LocalDateTime.now().minusMinutes(1));
        when(resetTokens.findByToken("reset-token")).thenReturn(Optional.of(token));

        assertThrows(AuthService.InvalidTokenException.class,
                () -> authService.confirmPasswordReset("reset-token", "new-password"));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void withdrawalDeletesTheAccountData() {
        User user = new User("Haruki", "a@example.com", "hash");
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        authService.withdraw(42L);

        verify(accountDeletion).deleteUserData(42L);
        verify(userRepository, never()).save(any());
    }
}
