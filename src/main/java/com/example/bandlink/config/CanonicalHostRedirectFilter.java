package com.example.bandlink.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Render keeps the default *.onrender.com hostname reachable even after a custom domain is
 * attached - there is no way to turn it off at the platform level - so anyone who still lands on
 * it is sent to the real domain instead. /robots.txt is left alone because that is also Render's
 * own healthCheckPath (render.yaml): if it started redirecting, Render would read the 3xx as the
 * deploy being unhealthy and take the service down.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CanonicalHostRedirectFilter implements Filter {
    private static final String LEGACY_HOST = "band-link.onrender.com";
    private static final String CANONICAL_ORIGIN = "https://band-link.jp";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String uri = request.getRequestURI();
        if (LEGACY_HOST.equalsIgnoreCase(request.getServerName()) && !"/robots.txt".equals(uri)) {
            String query = request.getQueryString();
            // 308 (not sendRedirect's 302) so a POST body/method survives the redirect for anyone
            // mid-flow on the old host.
            response.setStatus(308);
            response.setHeader("Location", CANONICAL_ORIGIN + uri + (query != null ? "?" + query : ""));
            return;
        }
        chain.doFilter(req, res);
    }
}
