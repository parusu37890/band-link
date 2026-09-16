package com.example.bandlink.dto;

import com.example.bandlink.entity.User;
import com.example.bandlink.entity.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ProfileResponseTest {

    /**
     * Regression for a bug found via ST-019: the public profile page (/users/{id}) never showed
     * "online" at all, even for someone active moments ago, because ProfileResponse's online flag
     * never consulted lastSeenAt/isOnline the way the posts listing (PostResponse.online) already
     * did. A user active 1 minute ago but who last explicitly logged in 5 days ago used to show a
     * stale day-bucket instead of "online now" on their own profile page, inconsistent with how the
     * same person appeared in search results.
     *
     * lastLoginAt is sent raw (not bucketed) so the client computes the same exact relative time
     * shown in the listing (ui.js loginRelativeTime) instead of a separately-worded day-bucket that
     * read like a different, staler answer for the same person.
     */
    @Test
    void onlineReflectsRecentActivityRegardlessOfStaleLastLogin() {
        LocalDateTime lastLogin = LocalDateTime.now().minusDays(5);
        User user = new User("Recently Active", "recent@example.com", "hash");
        user.touchLogin(lastLogin);
        user.touchSeen(LocalDateTime.now().minusMinutes(1));

        ProfileResponse response = ProfileResponse.from(user);

        assertTrue(response.online(), "a user seen 1 minute ago must be reported online");
        assertEquals(lastLogin, response.lastLoginAt(),
                "the raw timestamp is still sent so the client can show it if not online");
    }

    @Test
    void offlineWhenLastSeenIsOutsideTheOnlineWindow() {
        User user = new User("Not Recently Active", "notrecent@example.com", "hash");
        user.touchLogin(LocalDateTime.now().minusDays(1));
        user.touchSeen(LocalDateTime.now().minusMinutes(30));

        assertFalse(ProfileResponse.from(user).online());
    }

    @Test
    void suspendedAccountIsNeverShownOnlineEvenIfRecentlySeen() {
        User user = new User("Suspended", "suspended@example.com", "hash");
        user.setStatus(UserStatus.SUSPENDED);
        user.touchSeen(LocalDateTime.now().minusSeconds(5));

        assertFalse(ProfileResponse.from(user).online());
    }
}
