package com.example.bandlink.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ActivitySignalTest {

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
}
