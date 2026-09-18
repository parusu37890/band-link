package com.example.bandlink.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.example.bandlink.entity.User;
import com.example.bandlink.repository.UserRepository;
import jakarta.servlet.FilterChain;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.mockito.verification.VerificationMode;

/**
 * Policy reversal from this filter's original version: an unverified session used to be locked
 * out of the board entirely, including browsing and search (requirements.md 3 at the time, backed
 * by docs/test-results/2026-09-15-nft-004-005-006-013.md). That left a freshly registered person
 * worse off than someone who never signed in at all - an anonymous visitor could already read the
 * same listings. This now mirrors the anonymous read-only surface (SecurityConfig's own permitAll
 * GET rules) while keeping every write path and every private screen behind the gate.
 */
class EmailVerificationGateFilterTest {
    private final UserRepository users = mock(UserRepository.class);
    private final EmailVerificationGateFilter filter = new EmailVerificationGateFilter(users);

    @AfterEach
    void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test
    void unverifiedCanBrowseTheSameReadOnlyBoardAnAnonymousVisitorSees() throws Exception {
        authenticatedUnverifiedUser();
        for (String path : List.of("/", "/posts", "/posts/42", "/users/7")) {
            FilterChain chain = mock(FilterChain.class);
            filter.doFilter(request(path), new MockHttpServletResponse(), chain);
            verify(chain, description(path)).doFilter(any(), any());
        }
        for (String path : List.of("/api/masters", "/api/posts/page", "/api/posts/42", "/api/posts/42/images", "/api/users/7")) {
            FilterChain chain = mock(FilterChain.class);
            filter.doFilter(request(path), new MockHttpServletResponse(), chain);
            verify(chain, description(path)).doFilter(any(), any());
        }
    }

    @Test
    void unverifiedPrivatePageIsRedirectedToVerification() throws Exception {
        authenticatedUnverifiedUser();
        MockHttpServletRequest request = request("/messages");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals("/verify-email", response.getRedirectedUrl());
        verifyNoInteractions(chain);
    }

    @Test
    void unverifiedPrivateApiReceivesStructuredForbiddenResponse() throws Exception {
        authenticatedUnverifiedUser();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("/api/messages/conversations"), response, mock(FilterChain.class));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("EMAIL_NOT_VERIFIED"));
    }

    // Read-only browsing is allowed by method, not by path alone: posting or messaging still needs
    // a verified email even though GET /api/posts/page is now open to an unverified session.
    @Test
    void unverifiedWriteToAnOtherwiseReadableApiIsStillBlocked() throws Exception {
        authenticatedUnverifiedUser();
        MockHttpServletRequest request = request("/api/posts/page");
        request.setMethod("POST");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("EMAIL_NOT_VERIFIED"));
    }

    @Test
    void verificationPageRemainsReachable() throws Exception {
        authenticatedUnverifiedUser();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request("/verify-email"), new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    void resendVerificationRemainsReachable() throws Exception {
        authenticatedUnverifiedUser();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request("/api/auth/verify-email/resend"), new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
    }

    private MockHttpServletRequest request(String path) {
        // The no-arg constructor leaves getMethod() null, which the filter's own GET check would
        // then treat as "not GET" - a test-only gap, not something a real servlet container does.
        return new MockHttpServletRequest("GET", path);
    }

    private VerificationMode description(String path) { return times(1).description("path: " + path); }

    private void authenticatedUnverifiedUser() {
        User user = new User("Member", "member@example.com", "hash");
        when(users.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("member@example.com", null,
                        List.of()));
    }
}
