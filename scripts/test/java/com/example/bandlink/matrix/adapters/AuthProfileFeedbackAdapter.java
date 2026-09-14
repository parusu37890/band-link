package com.example.bandlink.matrix.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.bandlink.dto.FeedbackRequest;
import com.example.bandlink.dto.ProfileUpdateRequest;
import com.example.bandlink.dto.RegisterRequest;
import com.example.bandlink.entity.Feedback;
import com.example.bandlink.entity.FeedbackType;
import com.example.bandlink.entity.User;
import com.example.bandlink.matrix.ReleaseCase;
import com.example.bandlink.repository.GenreRepository;
import com.example.bandlink.repository.FeedbackRepository;
import com.example.bandlink.repository.PartRepository;
import com.example.bandlink.repository.PrefectureRepository;
import com.example.bandlink.repository.StanceRepository;
import com.example.bandlink.repository.UserRepository;
import com.example.bandlink.service.FeedbackService;
import com.example.bandlink.service.ImageStorageService;
import com.example.bandlink.service.ProfileService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.web.multipart.MultipartFile;

/**
 * Bean Validation adapter for Authentication, Profile, and Feedback inventory rows.
 *
 * <p>Every input state owned by this adapter constructs the application's real request DTO and
 * asserts its complete set of invalid properties. Repository and binary-file boundaries covered
 * by an input state are also checked through the real service implementation.</p>
 */
public final class AuthProfileFeedbackAdapter {
    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();
    private static final Set<Long> ONE_ID = Set.of(1L);

    private AuthProfileFeedbackAdapter() {
    }

    /** Executes the concrete DTO assertion for one release inventory row. */
    public static void execute(ReleaseCase row) {
        if (row == null) {
            throw new AssertionError("release case must not be null");
        }
        switch (row.feature()) {
            case "Authentication" -> executeAuthentication(row);
            case "Profile" -> executeProfile(row);
            case "Feedback" -> executeFeedback(row);
            default -> throw unsupported(row, "feature");
        }
    }

    private static void executeAuthentication(ReleaseCase row) {
        switch (row.inputState()) {
            case "empty" -> assertViolations(row,
                    new RegisterRequest("", "", "", null, null, null,
                            Set.of(), Set.of(), Set.of(), Set.of()),
                    "username", "email", "password", "age", "experienceYears", "gender",
                    "partIds", "genreIds", "stanceIds", "prefectureIds");
            case "invalid" -> assertViolations(row,
                    register("x", "not-an-email", "1234567", 20, 1, "その他"),
                    "email", "password", "gender");
            case "min" -> assertValid(row,
                    register("a", "a@example.test", "12345678", 0, 0, "男性"));
            case "max" -> assertValid(row,
                    register("名".repeat(80), emailOfLength(320), "p".repeat(128),
                            120, 100, "女性"));
            case "over" -> assertViolations(row,
                    register("名".repeat(81), emailOfLength(321), "p".repeat(129),
                            121, 101, "男性"),
                    "username", "email", "password", "age", "experienceYears");
            case "normal" -> assertValid(row,
                    register("通常ユーザー", "normal@example.test", "12345678",
                            20, 1, "男性"));
            default -> throw unsupported(row, "Authentication inputState");
        }
    }

    private static void executeProfile(ReleaseCase row) {
        switch (row.inputState()) {
            case "complete" -> assertValid(row, profile(
                    "QAユーザー", "男性", "自己紹介", 20, 1,
                    "https://example.test/video", "https://youtube.com/watch?v=qa",
                    "https://tiktok.com/@qa", "https://soundcloud.com/qa",
                    "https://open.spotify.com/artist/qa", "https://music.apple.com/qa",
                    ONE_ID, ONE_ID, ONE_ID, ONE_ID));
            case "empty" -> assertViolations(row, profile(
                    "", null, null, null, null,
                    null, null, null, null, null, null,
                    Set.of(), Set.of(), Set.of(), Set.of()),
                    "username", "gender", "age", "experienceYears",
                    "partIds", "genreIds", "stanceIds", "prefectureIds");
            case "invalid-url" -> assertViolations(row, profile(
                    "QA", "男性", null, 20, 1,
                    "u".repeat(1001), null, null, null, null, null,
                    ONE_ID, ONE_ID, ONE_ID, ONE_ID),
                    "videoUrl");
            case "max" -> assertValid(row, profile(
                    "名".repeat(80), "女性", "紹".repeat(1000), 120, 100,
                    "v".repeat(1000), "y".repeat(1000), "t".repeat(1000),
                    "s".repeat(1000), "p".repeat(1000), "a".repeat(1000),
                    Set.of(1L, 2L, 3L, 4L, 5L), Set.of(1L, 2L, 3L),
                    Set.of(1L), Set.of(1L, 2L, 3L)));
            case "min" -> assertValid(row, profile(
                    "a", "男性", "", 0, 0,
                    "", "", "", "", "", "",
                    ONE_ID, ONE_ID, ONE_ID, ONE_ID));
            case "unknown-master" -> assertUnknownMasterRejected(row);
            default -> throw unsupported(row, "Profile inputState");
        }
    }

    private static void executeFeedback(ReleaseCase row) {
        switch (row.inputState()) {
            case "body-boundary" -> assertFeedbackAccepted(row, FeedbackType.CONTACT,
                    new FeedbackRequest("お".repeat(3000), null));
            case "contact" -> assertFeedbackAccepted(row, FeedbackType.CONTACT,
                    new FeedbackRequest("運営へのお問い合わせです。", null));
            case "feature" -> assertFeedbackAccepted(row, FeedbackType.FEATURE_REQUEST,
                    new FeedbackRequest("機能改善の要望です。", null));
            case "empty-body" -> assertFeedbackRejected(row,
                    new FeedbackRequest("", null), "message");
            case "invalid-url" -> assertFeedbackRejected(row,
                    new FeedbackRequest("お問い合わせ", "u".repeat(1001)), "imageUrl");
            case "jpeg" -> assertFeedbackAccepted(row, FeedbackType.CONTACT,
                    new FeedbackRequest("JPEG添付", "/uploads/test.jpg"));
            case "no-image" -> assertFeedbackAccepted(row, FeedbackType.CONTACT,
                    new FeedbackRequest("画像なし", null));
            case "oversize" -> assertOversizeImageRejected(row);
            case "png" -> assertFeedbackAccepted(row, FeedbackType.CONTACT,
                    new FeedbackRequest("PNG添付", "/uploads/test.png"));
            case "webp" -> assertFeedbackAccepted(row, FeedbackType.CONTACT,
                    new FeedbackRequest("WebP添付", "/uploads/test.webp"));
            default -> throw unsupported(row, "Feedback inputState");
        }
    }

    private static RegisterRequest register(String username, String email, String password,
                                             Integer age, Integer experienceYears, String gender) {
        return new RegisterRequest(username, email, password, age, experienceYears, gender,
                ONE_ID, ONE_ID, ONE_ID, ONE_ID);
    }

    private static ProfileUpdateRequest profile(
            String username, String gender, String bio, Integer age, Integer experienceYears,
            String videoUrl, String youtubeUrl, String tiktokUrl, String soundcloudUrl,
            String spotifyUrl, String appleMusicUrl, Set<Long> partIds, Set<Long> genreIds,
            Set<Long> stanceIds, Set<Long> prefectureIds) {
        return new ProfileUpdateRequest(username, gender, bio, age, experienceYears,
                videoUrl, youtubeUrl, tiktokUrl, soundcloudUrl, spotifyUrl, appleMusicUrl,
                partIds, genreIds, stanceIds, prefectureIds);
    }

    private static String emailOfLength(int length) {
        String domain = String.join(".",
                "b".repeat(63), "c".repeat(63), "d".repeat(63), "e".repeat(63));
        return "a".repeat(length - domain.length() - 1) + "@" + domain;
    }

    private static void assertUnknownMasterRejected(ReleaseCase row) {
        ProfileUpdateRequest request = profile(
                "QA", "男性", null, 20, 1,
                null, null, null, null, null, null,
                Set.of(Long.MAX_VALUE), Set.of(Long.MAX_VALUE),
                Set.of(Long.MAX_VALUE), Set.of(Long.MAX_VALUE));
        assertValid(row, request);

        UserRepository users = mock(UserRepository.class);
        PartRepository parts = mock(PartRepository.class);
        GenreRepository genres = mock(GenreRepository.class);
        StanceRepository stances = mock(StanceRepository.class);
        PrefectureRepository prefectures = mock(PrefectureRepository.class);
        when(users.findById(1L)).thenReturn(Optional.of(
                new User("QA", "qa@example.test", "hash")));
        when(parts.findAllById(request.partIds())).thenReturn(java.util.List.of());
        ProfileService service = new ProfileService(
                users, parts, genres, stances, prefectures);

        assertThrows(IllegalArgumentException.class, () -> service.update(1L, request),
                () -> row.testId() + " must reject unknown master IDs");
    }

    private static void assertOversizeImageRejected(ReleaseCase row) {
        FeedbackRequest request = new FeedbackRequest(
                "容量超過画像", "/uploads/feedback/oversize.png");
        assertValid(row, request);

        MultipartFile image = mock(MultipartFile.class);
        when(image.isEmpty()).thenReturn(false);
        when(image.getSize()).thenReturn(5L * 1024 * 1024 + 1);
        when(image.getContentType()).thenReturn("image/png");
        assertThrows(IllegalArgumentException.class,
                () -> new ImageStorageService().store(image),
                () -> row.testId() + " must reject an image over 5 MiB");
    }

    private static void assertFeedbackAccepted(
            ReleaseCase row, FeedbackType type, FeedbackRequest request) {
        assertValid(row, request);
        User user = new User("QA", "qa@example.test", "hash");
        UserRepository users = mock(UserRepository.class);
        FeedbackRepository feedback = mock(FeedbackRepository.class);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(feedback.save(any(Feedback.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Feedback saved = new FeedbackService(users, feedback).create(1L, type, request);
        assertEquals(type, saved.getType(), () -> row.testId() + " feedback type differs");
        assertEquals(request.message().trim(), saved.getMessageText(),
                () -> row.testId() + " feedback message differs");
        assertEquals(request.imageUrl(), saved.getImageUrl(),
                () -> row.testId() + " feedback image differs");
    }

    private static void assertFeedbackRejected(
            ReleaseCase row, FeedbackRequest request, String... expectedProperties) {
        assertViolations(row, request, expectedProperties);
        UserRepository users = mock(UserRepository.class);
        FeedbackRepository feedback = mock(FeedbackRepository.class);
        assertThrows(IllegalArgumentException.class,
                () -> new FeedbackService(users, feedback)
                        .create(1L, FeedbackType.CONTACT, request),
                () -> row.testId() + " invalid feedback must be rejected");
    }

    private static void assertValid(ReleaseCase row, Object request) {
        assertViolations(row, request);
    }

    private static void assertViolations(ReleaseCase row, Object request,
                                         String... expectedProperties) {
        Set<String> actual = VALIDATOR.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
        Set<String> expected = Set.of(expectedProperties);
        assertEquals(expected, actual,
                () -> row.testId() + " " + row.feature() + "/" + row.inputState()
                        + " validation properties differ");
    }

    private static AssertionError unsupported(ReleaseCase row, String dimension) {
        return new AssertionError(row.testId() + " has unsupported " + dimension + ": "
                + ("feature".equals(dimension) ? row.feature() : row.inputState()));
    }
}
