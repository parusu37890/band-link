package com.example.bandlink.config;

import com.example.bandlink.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Keeps an authenticated account on the email verification screen until its link is used. */
public class EmailVerificationGateFilter extends OncePerRequestFilter {
    private static final Set<String> PAGE_ALLOWLIST = Set.of("/verify-email");
    private final UserRepository users;

    public EmailVerificationGateFilter(UserRepository users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!isAllowed(request)) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!isAuthenticated(authentication) || !isUnverified(authentication.getName())) {
                filterChain.doFilter(request, response);
                return;
            }
            if (request.getRequestURI().startsWith("/api/")) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":\"EMAIL_NOT_VERIFIED\",\"message\":\"メールアドレスの確認が完了するまで、この操作は利用できません。\"}");
            } else {
                response.sendRedirect("/verify-email");
            }
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isUnverified(String email) {
        return users.findByEmail(email).map(user -> !user.isEmailVerified()).orElse(false);
    }

    private boolean isAllowed(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (PAGE_ALLOWLIST.contains(path)) return true;
        if (path.equals("/api/auth/verify-email") || path.equals("/api/auth/verify-email/resend") || path.equals("/api/auth/logout")
                || path.equals("/api/auth/me") || path.equals("/api/csrf")) return true;
        return path.startsWith("/css/") || path.startsWith("/js/")
                || path.startsWith("/assets/") || path.startsWith("/uploads/")
                || path.equals("/error");
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
