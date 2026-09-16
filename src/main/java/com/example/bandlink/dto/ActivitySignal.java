package com.example.bandlink.dto;

import java.time.Duration;
import java.time.LocalDateTime;

/** Whether a poster currently counts as online, for the presence dot shown next to them. */
public final class ActivitySignal {
    private ActivitySignal() {}

    /** How recently someone must have been on the site to still count as here. */
    private static final Duration ONLINE_WITHIN = Duration.ofMinutes(5);

    /**
     * Whether to show the poster as online. Reads lastSeenAt, not lastLoginAt: sign-in time says
     * when someone arrived, which can be days before or hours after they were actually reading.
     * A boolean is all that leaves the server — the timestamp behind it stays here.
     */
    public static boolean isOnline(LocalDateTime lastSeenAt) {
        if (lastSeenAt == null) return false;
        Duration since = Duration.between(lastSeenAt, LocalDateTime.now());
        return !since.isNegative() && since.compareTo(ONLINE_WITHIN) <= 0;
    }
}
