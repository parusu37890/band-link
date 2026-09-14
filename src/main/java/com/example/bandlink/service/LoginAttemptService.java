package com.example.bandlink.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * SEC-013: found via Playwright MCP that nothing in the login path throttled repeated failed
 * attempts against the same account - 15 consecutive wrong-password POSTs to /api/auth/login all
 * came back as the identical 401 in roughly the same ~55ms, and the correct password still logged
 * in immediately afterward with no lockout at all. This is a deliberately minimal, in-memory,
 * per-instance counter (this app runs as a single instance; a distributed/Redis-backed limiter or
 * a per-IP scheme is a larger product decision than a bug fix should make unilaterally) rather
 * than a general-purpose rate limiter.
 *
 * Keyed by the raw normalized email that was attempted, not by whether it resolves to a real
 * account: AuthController.login()'s existing comment on hideUserNotFoundExceptions already
 * explains why a wrong-password reply and an unknown-email reply must stay indistinguishable, and
 * locking out a nonexistent address on the exact same schedule as a real one preserves that -
 * an attacker probing many candidate emails sees the same 429 after MAX_ATTEMPTS either way.
 */
@Service
public class LoginAttemptService {
    public static final int MAX_ATTEMPTS = 5;
    public static final Duration LOCKOUT = Duration.ofMinutes(15);

    private static final class State {
        int count;
        Instant lockedUntil;
    }

    private final ConcurrentHashMap<String, State> byEmail = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptService() { this(Clock.systemDefaultZone()); }

    LoginAttemptService(Clock clock) { this.clock = clock == null ? Clock.systemDefaultZone() : clock; }

    public boolean isLocked(String email) {
        State state = byEmail.get(key(email));
        return state != null && state.lockedUntil != null && state.lockedUntil.isAfter(clock.instant());
    }

    public synchronized void recordFailure(String email) {
        State state = byEmail.computeIfAbsent(key(email), ignored -> new State());
        if (state.lockedUntil != null && state.lockedUntil.isAfter(clock.instant())) return;
        state.count++;
        if (state.count >= MAX_ATTEMPTS) state.lockedUntil = clock.instant().plus(LOCKOUT);
    }

    public void recordSuccess(String email) {
        byEmail.remove(key(email));
    }

    private String key(String email) { return email == null ? "" : email.trim().toLowerCase(Locale.ROOT); }
}
