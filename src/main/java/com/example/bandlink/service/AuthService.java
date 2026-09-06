package com.example.bandlink.service;

import com.example.bandlink.dto.RegisterRequest;
import com.example.bandlink.dto.UserResponse;
import com.example.bandlink.entity.User;
import com.example.bandlink.entity.EmailVerificationToken;
import com.example.bandlink.entity.PasswordResetToken;
import com.example.bandlink.repository.EmailVerificationTokenRepository;
import com.example.bandlink.repository.PasswordResetTokenRepository;
import com.example.bandlink.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationTokenRepository verificationTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final MailService mail;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       EmailVerificationTokenRepository verificationTokens,
                       PasswordResetTokenRepository resetTokens, MailService mail) {
        this(userRepository, passwordEncoder, verificationTokens, resetTokens, mail, Clock.systemDefaultZone());
    }
    AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                EmailVerificationTokenRepository verificationTokens,
                PasswordResetTokenRepository resetTokens, MailService mail, Clock clock) {
        this.userRepository = userRepository; this.passwordEncoder = passwordEncoder;
        this.verificationTokens = verificationTokens; this.resetTokens = resetTokens; this.mail = mail;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }
        User user = userRepository.save(new User(request.username().trim(), email, passwordEncoder.encode(request.password())));
        // The token used to be created and then told to nobody (requirements 7章).
        String token = UUID.randomUUID().toString();
        verificationTokens.save(new EmailVerificationToken(user, token, now().plusHours(24)));
        mail.sendVerification(email, token);
        return UserResponse.from(user);
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken stored = verificationTokens.findByToken(token.trim())
                .orElseThrow(() -> new InvalidTokenException());
        if (!stored.isUsableAt(now())) throw new InvalidTokenException();
        stored.getUser().setEmailVerifiedAt(now());
        stored.setUsedAt(now());
    }

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email.trim().toLowerCase(java.util.Locale.ROOT)).ifPresent(user -> {
            if (user.getStatus() != com.example.bandlink.entity.UserStatus.WITHDRAWN) {
                String token = UUID.randomUUID().toString();
                resetTokens.save(new PasswordResetToken(user, token, now().plusHours(24)));
                mail.sendPasswordReset(user.getEmail(), token);
            }
        });
    }

    @Transactional
    public void confirmPasswordReset(String token, String newPassword) {
        PasswordResetToken stored = resetTokens.findByToken(token.trim())
                .orElseThrow(() -> new InvalidTokenException());
        if (!stored.isUsableAt(now())) throw new InvalidTokenException();
        stored.getUser().setPasswordHash(passwordEncoder.encode(newPassword));
        stored.setUsedAt(now());
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new InvalidTokenException());
        if (user.getStatus() == com.example.bandlink.entity.UserStatus.WITHDRAWN) return;
        user.setStatus(com.example.bandlink.entity.UserStatus.WITHDRAWN);
    }

    private LocalDateTime now() { return LocalDateTime.now(clock); }

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException() { super("トークンが無効または期限切れです"); }
    }

    public static class EmailAlreadyUsedException extends RuntimeException {
        public EmailAlreadyUsedException() { super("このメールアドレスは既に登録されています"); }
    }
}
