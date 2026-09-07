package com.example.bandlink.config;

import com.example.bandlink.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Records that a signed-in account is currently using the site, so the board can mark a poster as
 * online.
 *
 * lastLoginAt cannot answer that question: it moves only when someone signs in, so a person who
 * signed in this morning and is reading right now looks stale, and one who signed in a minute ago
 * and closed the tab looks present. Presence needs its own timestamp.
 *
 * Writing on every request would mean a database write per page view, so the timestamp is only
 * moved once it is older than WRITE_EVERY. That is coarser than the five minutes ActivitySignal
 * treats as online, so the window is never cut short by the throttle.
 */
public class LastSeenFilter extends OncePerRequestFilter {
    private static final Duration WRITE_EVERY = Duration.ofMinutes(1);
    private final UserRepository users;

    public LastSeenFilter(UserRepository users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isAuthenticated(authentication)) touch(authentication.getName());
        filterChain.doFilter(request, response);
    }

    /** Static files are requested in bursts alongside the page that needs them; one touch is enough. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/assets/")
                || path.startsWith("/uploads/") || path.equals("/error");
    }

    private void touch(String email) {
        LocalDateTime now = LocalDateTime.now();
        users.findByEmail(email).ifPresent(user -> {
            LocalDateTime seen = user.getLastSeenAt();
            if (seen != null && seen.isAfter(now.minus(WRITE_EVERY))) return;
            user.touchSeen(now);
            users.save(user);
        });
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
