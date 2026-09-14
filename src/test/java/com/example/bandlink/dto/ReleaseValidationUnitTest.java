package com.example.bandlink.dto;

import com.example.bandlink.entity.ActivityFrequency;
import com.example.bandlink.entity.AgeRange;
import com.example.bandlink.entity.PostType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Release acceptance assertions. Some deliberately expose current gaps; Runa must record a
 * failure rather than weaken these limits to match the implementation.
 */
class ReleaseValidationUnitTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void codeUt026_registrationTextBoundaries() {
        assertValid(register("名", "a@example.test", "12345678"));
        assertValid(register("名".repeat(80), "a@example.test", "p".repeat(128)));
        assertValid(register("名", emailOfLength(320), "12345678"));
        assertInvalid(register("", "bad", "1234567"), "username", "email", "password");
        assertInvalid(register("名".repeat(81), "a@example.test", "p".repeat(129)), "username", "password");
        assertInvalid(register("名", emailOfLength(321), "12345678"), "email");
    }

    @Test
    void codeUt027_ageAndExperienceBoundaries() {
        assertValid(register("境界", "edge@example.test", "12345678", 0, 0, "男性"));
        assertValid(register("境界", "edge@example.test", "12345678", 120, 100, "女性"));
        assertInvalid(register("境界", "edge@example.test", "12345678", -1, -1, "男性"), "age", "experienceYears");
        assertInvalid(register("境界", "edge@example.test", "12345678", 121, 101, "女性"), "age", "experienceYears");
    }

    @Test
    void codeUt028_genderIsExactlyTwoReleaseLabels() {
        assertValid(register("性別", "gender@example.test", "12345678", 20, 1, "男性"));
        assertValid(register("性別", "gender@example.test", "12345678", 20, 1, "女性"));
        for (String value : new String[]{"男", "女", "その他", " 男性 ", ""}) {
            assertInvalid(register("性別", "gender@example.test", "12345678", 20, 1, value), "gender");
        }
        assertInvalid(register("性別", "gender@example.test", "12345678", 20, 1, null), "gender");
    }

    @Test
    void codeUt029_profileRequiresMastersAndAtMostThreePrefectures() {
        assertValid(profile(Set.of(1L), Set.of(1L), Set.of(1L), Set.of(1L, 2L, 3L)));
        assertInvalid(new ProfileUpdateRequest(" ", "男性", null, 20, 1, null, null, null, null, null, null,
                Set.of(1L), Set.of(1L), Set.of(1L), Set.of(1L)), "username");
        assertInvalid(profile(Set.of(), Set.of(1L), Set.of(1L), Set.of(1L)), "partIds");
        assertInvalid(profile(Set.of(1L), Set.of(), Set.of(1L), Set.of(1L)), "genreIds");
        assertInvalid(profile(Set.of(1L), Set.of(1L), Set.of(), Set.of(1L)), "stanceIds");
        assertInvalid(profile(Set.of(1L), Set.of(1L), Set.of(1L), Set.of()), "prefectureIds");
        assertInvalid(profile(Set.of(1L), Set.of(1L), Set.of(1L), Set.of(1L, 2L, 3L, 4L)), "prefectureIds");
    }

    @Test
    void codeUt030_postTextBoundaries() {
        assertValid(post("題", "本", "地"));
        assertValid(post("題".repeat(30), "本".repeat(500), "地".repeat(100)));
        assertInvalid(post("", "", null), "title", "content");
        assertInvalid(post("題".repeat(31), "本".repeat(501), "地".repeat(101)), "title", "content", "areaSub");
    }

    @Test
    void codeUt031_postSelectionRequiresOneOfEachAndAtMostThreePrefectures() {
        // requirements.md: parts/genres/stances need one or more with no documented ceiling
        // ("それぞれ1つ以上選ぶ", "複数選択可能"); only 活動エリア is capped, at 3. A generous, large
        // but uncapped selection must stay valid on every field except prefectures.
        assertValid(post(Set.of(1L, 2L, 3L, 4L, 5L, 6L), Set.of(1L, 2L, 3L, 4L), Set.of(1L, 2L), Set.of(1L, 2L, 3L)));
        assertInvalid(post(Set.of(), Set.of(1L), Set.of(1L), Set.of(1L)), "partIds");
        assertInvalid(post(Set.of(1L), Set.of(), Set.of(1L), Set.of(1L)), "genreIds");
        assertInvalid(post(Set.of(1L), Set.of(1L), Set.of(), Set.of(1L)), "stanceIds");
        assertInvalid(post(Set.of(1L), Set.of(1L), Set.of(1L), Set.of()), "prefectureIds");
        assertInvalid(post(Set.of(1L), Set.of(1L), Set.of(1L), Set.of(1L, 2L, 3L, 4L)), "prefectureIds");
    }

    private RegisterRequest register(String name, String email, String password) {
        return register(name, email, password, 20, 1, "男性");
    }

    private RegisterRequest register(String name, String email, String password, Integer age,
                                     Integer experience, String gender) {
        return new RegisterRequest(name, email, password, age, experience, gender,
                Set.of(1L), Set.of(1L), Set.of(1L), Set.of(1L));
    }

    private String emailOfLength(int length) {
        String domain = String.join(".", "b".repeat(63), "c".repeat(63), "d".repeat(63), "e".repeat(63));
        return "a".repeat(length - domain.length() - 1) + "@" + domain;
    }

    private ProfileUpdateRequest profile(Set<Long> parts, Set<Long> genres, Set<Long> stances,
                                         Set<Long> prefectures) {
        return new ProfileUpdateRequest("QA", "男性", "紹介", 20, 1,
                null, null, null, null, null, null, parts, genres, stances, prefectures);
    }

    private PostRequests.Create post(String title, String content, String area) {
        return new PostRequests.Create(PostType.MEMBER_WANTED, title, content, area,
                Set.of(1L), Set.of(1L), Set.of(1L), Set.of(1L), Set.of(AgeRange.ANY), ActivityFrequency.WEEKLY_1);
    }

    private PostRequests.Create post(Set<Long> parts, Set<Long> genres, Set<Long> stances,
                                     Set<Long> prefectures) {
        return new PostRequests.Create(PostType.MEMBER_WANTED, "題", "本文", "都内",
                parts, genres, stances, prefectures, Set.of(AgeRange.ANY), ActivityFrequency.WEEKLY_1);
    }

    private void assertValid(Object value) {
        assertTrue(validator.validate(value).isEmpty(), () -> validator.validate(value).toString());
    }

    private void assertInvalid(Object value, String... expectedProperties) {
        Set<String> actual = validator.validate(value).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
        for (String property : expectedProperties) {
            assertTrue(actual.contains(property), () -> property + " is not constrained; actual=" + actual);
        }
        assertEquals(Set.copyOf(java.util.List.of(expectedProperties)), actual,
                () -> "Unexpected constraint fields: " + actual);
    }
}
