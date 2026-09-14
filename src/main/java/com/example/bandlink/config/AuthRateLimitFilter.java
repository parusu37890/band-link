package com.example.bandlink.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Applies a small fixed-window limit to anonymous authentication endpoints.
 *
 * <p>The key deliberately excludes email addresses and tokens: keeping those values in memory or
 * logs would create a second copy of authentication data, and keying only by account would let an
 * attacker enumerate accounts through different throttling behaviour. The remote address and
 * endpoint produce the same response for existing and unknown accounts.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final Set<String> PROTECTED_PATHS = Set.of(
            "/login",
            "/api/auth/login",
            "/api/auth/verify-email",
            "/api/auth/verify-email/resend",
            "/api/auth/password-reset/request",
            "/api/auth/password-reset/confirm");
    private static final int MAX_TRACKED_KEYS = 10_000;

    private final int maximumRequests;
    private final Duration window;
    private final Clock clock;
    private final ConcurrentHashMap<Key, Window> windows = new ConcurrentHashMap<>();

    @Autowired
    public AuthRateLimitFilter(
            @Value("${app.security.auth-rate-limit.max-requests:20}") int maximumRequests,
            @Value("${app.security.auth-rate-limit.window-seconds:60}") long windowSeconds) {
        this(maximumRequests, Duration.ofSeconds(windowSeconds), Clock.systemUTC());
    }

    AuthRateLimitFilter(int maximumRequests, Duration window, Clock clock) {
        if (maximumRequests < 1) throw new IllegalArgumentException("maximumRequests must be positive");
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }
        this.maximumRequests = maximumRequests;
        this.window = window;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !PROTECTED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Instant now = clock.instant();
        Key requestedKey = new Key(request.getRemoteAddr(), request.getRequestURI());
        Key key = windows.containsKey(requestedKey) || windows.size() < MAX_TRACKED_KEYS
                ? requestedKey : new Key("overflow", request.getRequestURI());
        Window current = windows.compute(key, (ignored, previous) ->
                previous == null || !now.isBefore(previous.startedAt().plus(window))
                        ? new Window(now, 1)
                        : new Window(previous.startedAt(), previous.count() + 1));

        if (current.count() > maximumRequests) {
            long retryAfter = Math.max(1, Duration.between(now, current.startedAt().plus(window)).toSeconds());
            response.setStatus(429);
            response.setHeader("Retry-After", Long.toString(retryAfter));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":\"RATE_LIMITED\",\"message\":\"操作回数が多すぎます。しばらく待ってから再試行してください。\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private record Key(String remoteAddress, String path) {}
    private record Window(Instant startedAt, int count) {}
}
