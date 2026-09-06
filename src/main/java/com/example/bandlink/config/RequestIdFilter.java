package com.example.bandlink.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Tags every request with an id and records how it ended.
 *
 * requirements 13.3 asks each log line to carry the time, level, operation, request id, outcome and
 * duration. The id alone was being set before, so a request left no record of whether it succeeded
 * or how long it took, and Kibana had nothing to search per request.
 *
 * Only the method, the path template's raw path, the status and the elapsed time are recorded.
 * Query strings, headers, cookies and bodies are deliberately left out: 13.3 forbids logging
 * passwords, tokens, cookies, message bodies and whole request bodies, and a query string can carry
 * search keywords that belong to the person searching.
 */
@Component
public class RequestIdFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger("com.example.bandlink.access");

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String id = request.getHeader("X-Request-Id");
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        long startedAt = System.nanoTime();
        MDC.put("request_id", id);
        response.setHeader("X-Request-Id", id);
        try {
            chain.doFilter(req, res);
        } finally {
            long millis = (System.nanoTime() - startedAt) / 1_000_000;
            MDC.put("http_method", request.getMethod());
            MDC.put("http_path", request.getRequestURI());
            MDC.put("http_status", String.valueOf(response.getStatus()));
            MDC.put("duration_ms", String.valueOf(millis));
            log.info("request completed");
            MDC.clear();
        }
    }
}
