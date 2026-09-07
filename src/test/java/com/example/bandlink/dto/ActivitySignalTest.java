package com.example.bandlink.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ActivitySignalTest {

    @Test void bucketsRecentActivityAndHidesStaleOrUnknown() {
        LocalDateTime now = LocalDateTime.now();
        assertEquals("3日以内にログイン", ActivitySignal.of(now.minusHours(2)));
        assertEquals("3日以内にログイン", ActivitySignal.of(now.minusDays(3)));
        assertEquals("1週間以内にログイン", ActivitySignal.of(now.minusDays(5)));
        assertEquals("1か月以内にログイン", ActivitySignal.of(now.minusDays(20)));
        // Stale and unknown both read as "no recent sign-in" rather than shaming the account.
        assertNull(ActivitySignal.of(now.minusDays(200)));
        assertNull(ActivitySignal.of(null));
    }

    @Test void onlineReadsPresenceNotSignInTime() {
        LocalDateTime now = LocalDateTime.now();
        assertTrue(ActivitySignal.isOnline(now.minusSeconds(30)));
        assertTrue(ActivitySignal.isOnline(now.minusMinutes(4)));
        assertFalse(ActivitySignal.isOnline(now.minusMinutes(6)));
        assertFalse(ActivitySignal.isOnline(now.minusDays(1)));
        // Never seen, and a clock that ran backwards, both read as away rather than as present.
        assertFalse(ActivitySignal.isOnline(null));
        assertFalse(ActivitySignal.isOnline(now.plusHours(1)));
    }

    @Test void neverLeaksTheExactTimestamp() {
        String label = ActivitySignal.of(LocalDateTime.of(2026, 3, 4, 5, 6));
        assertTrue(label == null || java.util.List.of("3日以内にログイン", "1週間以内にログイン", "1か月以内にログイン").contains(label),
                "only the fixed buckets may be returned, never a formatted timestamp");
    }
}
