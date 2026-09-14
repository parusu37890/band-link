package com.example.bandlink.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SecurityBoundaryFilterTest {

    @Test
    void authEndpointsReturnTheSameRateLimitResponseWithoutUsingAccountData() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(2, Duration.ofMinutes(1), Clock.systemUTC());
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletResponse first = invoke(filter, chain, "/api/auth/login", "198.51.100.4");
        MockHttpServletResponse second = invoke(filter, chain, "/api/auth/login", "198.51.100.4");
        MockHttpServletResponse limited = invoke(filter, chain, "/api/auth/login", "198.51.100.4");

        assertEquals(200, first.getStatus());
        assertEquals(200, second.getStatus());
        assertEquals(429, limited.getStatus());
        assertTrue(limited.getContentAsString().contains("RATE_LIMITED"));
        assertFalse(limited.getContentAsString().contains("email"));
        assertTrue(Long.parseLong(limited.getHeader("Retry-After")) > 0);
        verify(chain, times(2)).doFilter(any(), any());
    }

    @Test
    void rateLimitIsIndependentByEndpointAndRemoteAddress() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(1, Duration.ofMinutes(1), Clock.systemUTC());
        FilterChain chain = mock(FilterChain.class);
        assertEquals(200, invoke(filter, chain, "/api/auth/login", "198.51.100.5").getStatus());
        assertEquals(429, invoke(filter, chain, "/api/auth/login", "198.51.100.5").getStatus());
        assertEquals(200, invoke(filter, chain, "/api/auth/password-reset/request", "198.51.100.5").getStatus());
        assertEquals(200, invoke(filter, chain, "/api/auth/login", "198.51.100.6").getStatus());
    }

    @Test
    void rateLimitWindowExpires() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-14T00:00:00Z"));
        AuthRateLimitFilter filter = new AuthRateLimitFilter(1, Duration.ofSeconds(30), clock);
        FilterChain chain = mock(FilterChain.class);
        assertEquals(200, invoke(filter, chain, "/api/auth/verify-email", "198.51.100.7").getStatus());
        assertEquals(429, invoke(filter, chain, "/api/auth/verify-email", "198.51.100.7").getStatus());
        clock.advance(Duration.ofSeconds(31));
        assertEquals(200, invoke(filter, chain, "/api/auth/verify-email", "198.51.100.7").getStatus());
    }

    @Test
    void maliciousRequestIdsAreReplacedBeforeResponseAndMdcUse() throws Exception {
        RequestIdFilter filter = new RequestIdFilter();
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/posts/page");
        String hostile = "probe\r\nERROR forged=true";
        request.addHeader("X-Request-Id", hostile);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        String returned = response.getHeader("X-Request-Id");
        assertNotEquals(hostile, returned);
        assertTrue(returned.matches("[0-9a-f-]{36}"));
        verify(chain).doFilter(request, response);
    }

    @Test
    void validBoundedRequestIdIsPreserved() throws Exception {
        RequestIdFilter filter = new RequestIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/posts/page");
        request.addHeader("X-Request-Id", "qa-request_2026.09-14");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, mock(FilterChain.class));
        assertEquals("qa-request_2026.09-14", response.getHeader("X-Request-Id"));
    }

    @Test
    void duplicateScalarParametersAreRejectedButListFiltersRemainValid() throws Exception {
        DuplicateParameterFilter filter = new DuplicateParameterFilter();
        FilterChain rejectedChain = mock(FilterChain.class);
        MockHttpServletRequest duplicate = new MockHttpServletRequest();
        duplicate.setRequestURI("/api/posts/page");
        duplicate.addParameter("limit", "10", "100");
        MockHttpServletResponse rejected = new MockHttpServletResponse();
        filter.doFilter(duplicate, rejected, rejectedChain);
        assertEquals(400, rejected.getStatus());
        verify(rejectedChain, never()).doFilter(any(), any());

        FilterChain allowedChain = mock(FilterChain.class);
        MockHttpServletRequest list = new MockHttpServletRequest();
        list.setRequestURI("/api/posts/page");
        list.addParameter("partIds", "1", "2");
        filter.doFilter(list, new MockHttpServletResponse(), allowedChain);
        verify(allowedChain).doFilter(any(), any());
    }

    private MockHttpServletResponse invoke(AuthRateLimitFilter filter, FilterChain chain,
                                           String path, String remoteAddress) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI(path);
        request.setRemoteAddr(remoteAddress);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> instant;
        MutableClock(Instant initial) { instant = new AtomicReference<>(initial); }
        void advance(Duration duration) { instant.updateAndGet(value -> value.plus(duration)); }
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant.get(); }
    }
}
