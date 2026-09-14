package com.example.bandlink.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Rejects HTTP parameter pollution for scalar inputs while preserving documented list filters. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class DuplicateParameterFilter extends OncePerRequestFilter {
    private static final Set<String> MULTI_VALUE_PARAMETERS = Set.of(
            "partIds", "genreIds", "stanceIds", "prefectureIds", "ageRanges", "activityFrequencies");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        boolean duplicateScalar = request.getParameterMap().entrySet().stream()
                .anyMatch(entry -> !MULTI_VALUE_PARAMETERS.contains(entry.getKey())
                        && entry.getValue() != null && entry.getValue().length > 1);
        if (duplicateScalar) {
            response.setStatus(400);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":\"INVALID_INPUT\",\"message\":\"入力内容を確認してください。\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
