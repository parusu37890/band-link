package com.example.bandlink.service;

import com.example.bandlink.entity.User;
import com.example.bandlink.entity.UserStatus;
import com.example.bandlink.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {
    @Mock UserRepository users; @Mock PartRepository parts; @Mock GenreRepository genres;
    @Mock StanceRepository stances; @Mock PrefectureRepository prefectures;

    @Test
    void getPublicReturns404StyleNotFoundInsteadOfInvalidInputForMissingUser() {
        ProfileService service = new ProfileService(users, parts, genres, stances, prefectures);
        when(users.findById(999999L)).thenReturn(Optional.empty());

        // A missing profile must surface as NOT_FOUND (mapped to HTTP 404 by ApiExceptionHandler),
        // not IllegalArgumentException (which the same handler maps to a misleading
        // "confirm your input" 400 INVALID_INPUT — there was nothing wrong with the request).
        assertThrows(NoSuchElementException.class, () -> service.getPublic(999999L));
    }

    @Test
    void getPublicReturns404StyleNotFoundForSuspendedOrWithdrawnUsers() {
        ProfileService service = new ProfileService(users, parts, genres, stances, prefectures);
        User suspended = new User("Suspended", "suspended@example.com", "hash");
        suspended.setStatus(UserStatus.SUSPENDED);
        when(users.findById(1L)).thenReturn(Optional.of(suspended));
        assertThrows(NoSuchElementException.class, () -> service.getPublic(1L));

        User withdrawn = new User("Withdrawn", "withdrawn@example.com", "hash");
        withdrawn.setStatus(UserStatus.WITHDRAWN);
        when(users.findById(2L)).thenReturn(Optional.of(withdrawn));
        assertThrows(NoSuchElementException.class, () -> service.getPublic(2L));
    }

    @Test
    void getPublicReturnsActiveUser() {
        ProfileService service = new ProfileService(users, parts, genres, stances, prefectures);
        User active = new User("Active", "active@example.com", "hash");
        when(users.findById(3L)).thenReturn(Optional.of(active));
        assertSame(active, service.getPublic(3L));
    }

    /**
     * SEC-003 (PW-H): update() loaded the user with a bare findById and never checked isActive(),
     * so a suspended account could still rewrite its own profile through PUT /api/users/me even
     * though requirements.md is explicit that suspension prohibits "投稿・メッセージ送信"
     * (posting and message sending) - and MessageService/PostService already enforce that for
     * their own writes. Profile edits were the one write path with no such check anywhere in its
     * call chain (not in ProfileController, not here).
     */
    @Test
    void updateRejectsASuspendedUsersProfileEditWithoutWritingAnything() {
        ProfileService service = new ProfileService(users, parts, genres, stances, prefectures);
        User suspended = new User("Suspended", "suspended@example.com", "hash");
        suspended.setStatus(UserStatus.SUSPENDED);
        when(users.findById(4L)).thenReturn(Optional.of(suspended));

        assertThrows(ProfileService.RuleViolationException.class,
                () -> service.update(4L, new com.example.bandlink.dto.ProfileUpdateRequest(
                        "name", "男性", "new bio", 20, 1, null, null, null, null, null, null,
                        java.util.Set.of(1L), java.util.Set.of(1L), java.util.Set.of(1L), java.util.Set.of(1L))));
        assertEquals("suspended@example.com", suspended.getEmail());
        assertNull(suspended.getBio());
    }

    @Test
    void updateAppliesFieldsForAnActiveUser() {
        ProfileService service = new ProfileService(users, parts, genres, stances, prefectures);
        User active = new User("Active", "active@example.com", "hash");
        when(users.findById(5L)).thenReturn(Optional.of(active));

        User result = service.update(5L, new com.example.bandlink.dto.ProfileUpdateRequest(
                "new name", "女性", "new bio", 25, 2, null, null, null, null, null, null,
                null, null, null, null));
        assertSame(active, result);
        assertEquals("new bio", active.getBio());
    }
}
