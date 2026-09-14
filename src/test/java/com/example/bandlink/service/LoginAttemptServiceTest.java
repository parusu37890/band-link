package com.example.bandlink.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SEC-013 (PW-H): before this service existed, nothing throttled repeated failed logins at all -
 * confirmed live against the running app (15 consecutive wrong-password POSTs to
 * /api/auth/login, same 401 every time, no delay, no lockout, and the correct password still
 * worked immediately after). These pin the counting/lockout/reset/expiry behaviour directly.
 */
class LoginAttemptServiceTest {
    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-09-14T00:00:00Z");
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test void staysUnlockedBeforeReachingTheThreshold() {
        var clock = new MutableClock();
        var service = new LoginAttemptService(clock);
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS - 1; i++) {
            service.recordFailure("qa@example.test");
            assertFalse(service.isLocked("qa@example.test"), "should not lock before the threshold, attempt " + (i + 1));
        }
    }

    @Test void locksOutOnceTheThresholdIsReachedAndStaysLockedUntilItExpires() {
        var clock = new MutableClock();
        var service = new LoginAttemptService(clock);
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) service.recordFailure("qa@example.test");
        assertTrue(service.isLocked("qa@example.test"));

        clock.now = clock.now.plus(LoginAttemptService.LOCKOUT.minusSeconds(1));
        assertTrue(service.isLocked("qa@example.test"), "still locked just before the window ends");

        clock.now = clock.now.plus(Duration.ofSeconds(2));
        assertFalse(service.isLocked("qa@example.test"), "unlocked once the window has fully elapsed");
    }

    @Test void aSuccessfulLoginResetsTheCounter() {
        var clock = new MutableClock();
        var service = new LoginAttemptService(clock);
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS - 1; i++) service.recordFailure("qa@example.test");
        service.recordSuccess("qa@example.test");
        service.recordFailure("qa@example.test");
        assertFalse(service.isLocked("qa@example.test"), "the counter should have been cleared by the success, not carried over");
    }

    @Test void locksOutANonexistentEmailOnTheExactSameScheduleAsARealOne() {
        // No enumeration signal: whether the email resolves to a real account is decided one
        // layer up (AuthController/BandLinkUserDetailsService), never by this service.
        var clock = new MutableClock();
        var service = new LoginAttemptService(clock);
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) service.recordFailure("no-such-account@example.test");
        assertTrue(service.isLocked("no-such-account@example.test"));
    }

    @Test void emailKeyIsCaseAndWhitespaceInsensitive() {
        var clock = new MutableClock();
        var service = new LoginAttemptService(clock);
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) service.recordFailure("  QA@Example.Test ");
        assertTrue(service.isLocked("qa@example.test"));
    }

    @Test void differentEmailsAreTrackedIndependently() {
        var clock = new MutableClock();
        var service = new LoginAttemptService(clock);
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) service.recordFailure("a@example.test");
        assertTrue(service.isLocked("a@example.test"));
        assertFalse(service.isLocked("b@example.test"));
    }
}
