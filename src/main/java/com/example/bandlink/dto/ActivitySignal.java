package com.example.bandlink.dto;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Coarse "is this person still around" label shown next to a poster.
 *
 * Deciding whether to write to a stranger depends on whether they still read their messages, but an
 * exact last-login timestamp exposes more than that decision needs. Buckets keep the useful signal
 * and drop the rest. Returns null once activity is old or unknown: absence reads as "no recent
 * sign-in" without singling anyone out.
 */
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

    public static String of(LocalDateTime lastLoginAt) {
        if (lastLoginAt == null) return null;
        long days = Duration.between(lastLoginAt, LocalDateTime.now()).toDays();
        if (days < 0) return null;
        if (days <= 3) return "3日以内にログイン";
        if (days <= 7) return "1週間以内にログイン";
        if (days <= 30) return "1か月以内にログイン";
        return null;
    }
}
