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
}
