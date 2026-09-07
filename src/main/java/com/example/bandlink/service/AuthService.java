package com.example.bandlink.service;

import com.example.bandlink.dto.RegisterRequest;
import com.example.bandlink.dto.UserResponse;
import com.example.bandlink.entity.User;
import com.example.bandlink.entity.EmailVerificationToken;
import com.example.bandlink.entity.PasswordResetToken;
import com.example.bandlink.repository.EmailVerificationTokenRepository;
import com.example.bandlink.repository.GenreRepository;
import com.example.bandlink.repository.PartRepository;
import com.example.bandlink.repository.PasswordResetTokenRepository;
import com.example.bandlink.repository.PrefectureRepository;
import com.example.bandlink.repository.StanceRepository;
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
    private final AccountDeletionService accountDeletion;
    private final MailService mail;
    private final PartRepository parts;
    private final GenreRepository genres;
    private final StanceRepository stances;
    private final PrefectureRepository prefectures;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       EmailVerificationTokenRepository verificationTokens,
                       PasswordResetTokenRepository resetTokens, MailService mail, AccountDeletionService accountDeletion) {
        this(userRepository, passwordEncoder, verificationTokens, resetTokens, mail, accountDeletion,
                null, null, null, null, Clock.systemDefaultZone());
    }
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       EmailVerificationTokenRepository verificationTokens,
                       PasswordResetTokenRepository resetTokens, MailService mail, AccountDeletionService accountDeletion,
                       PartRepository parts, GenreRepository genres, StanceRepository stances,
                       PrefectureRepository prefectures) {
        this(userRepository, passwordEncoder, verificationTokens, resetTokens, mail, accountDeletion,
                parts, genres, stances, prefectures, Clock.systemDefaultZone());
    }
    AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                EmailVerificationTokenRepository verificationTokens,
                PasswordResetTokenRepository resetTokens, MailService mail, AccountDeletionService accountDeletion,
                PartRepository parts, GenreRepository genres, StanceRepository stances,
                PrefectureRepository prefectures, Clock clock) {
        this.userRepository = userRepository; this.passwordEncoder = passwordEncoder;
        this.verificationTokens = verificationTokens; this.resetTokens = resetTokens;
        this.mail = mail; this.accountDeletion = accountDeletion;
        this.parts = parts; this.genres = genres; this.stances = stances; this.prefectures = prefectures;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }
        User user = new User(request.username().trim(), email, passwordEncoder.encode(request.password()));
        // Older service-level callers may still use the three-argument constructor. HTTP callers
        // are validated by RegisterRequest and always provide the required profile fields.
        if (request.age() != null) {
            user.setAge(request.age());
            user.setExperienceYears(request.experienceYears());
            user.setGender(request.gender());
            if (parts != null) user.getParts().addAll(parts.findAllById(request.partIds()));
            if (genres != null) user.getGenres().addAll(genres.findAllById(request.genreIds()));
            if (stances != null) user.getStances().addAll(stances.findAllById(request.stanceIds()));
            if (prefectures != null) {
                if (request.prefectureIds().size() > 3) throw new IllegalArgumentException("活動エリアは3つまでです。");
                user.getPrefectures().addAll(prefectures.findAllById(request.prefectureIds()));
            }
        }
        user = userRepository.save(user);
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

    /** Sends a fresh one-time confirmation link to the currently signed-in, unverified user. */
    @Transactional
    public void resendVerification(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new InvalidTokenException());
        if (user.isEmailVerified()) return;
        verificationTokens.findAllByUserIdAndUsedAtIsNull(userId).forEach(token -> token.setUsedAt(now()));
        String token = UUID.randomUUID().toString();
        verificationTokens.save(new EmailVerificationToken(user, token, now().plusHours(24)));
        mail.sendVerification(user.getEmail(), token);
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

    /** A voluntary withdrawal permanently removes the account and its owned data. */
    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new InvalidTokenException());
        if (user.getStatus() == com.example.bandlink.entity.UserStatus.WITHDRAWN) return;
        accountDeletion.deleteUserData(userId);
    }

    private LocalDateTime now() { return LocalDateTime.now(clock); }

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException() { super("トークンが無効または期限切れです"); }
    }

    public static class EmailAlreadyUsedException extends RuntimeException {
        public EmailAlreadyUsedException() { super("このメールアドレスは既に登録されています"); }
    }
}
