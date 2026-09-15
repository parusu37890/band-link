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

class EmailVerificationGateFilterTest {
    private final UserRepository users = mock(UserRepository.class);
    private final EmailVerificationGateFilter filter = new EmailVerificationGateFilter(users);

    @AfterEach
    void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test
    void unverifiedPageIsRedirectedToVerification() throws Exception {
        authenticatedUnverifiedUser();
        MockHttpServletRequest request = request("/posts");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals("/verify-email", response.getRedirectedUrl());
        verifyNoInteractions(chain);
    }

    @Test
    void unverifiedApiReceivesStructuredForbiddenResponse() throws Exception {
        authenticatedUnverifiedUser();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("/api/posts"), response, mock(FilterChain.class));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("EMAIL_NOT_VERIFIED"));
    }

    // Locks in the behavior described by /support's help copy: browsing and search are NOT
    // reachable before verification (requirements.md 3: an unverified session shows only the
    // verification screen, nothing else). The /support page used to claim the opposite
    // ("閲覧と検索は確認前でもできます") until this was found to contradict the filter - see
    // docs/test-results/2026-09-15-nft-004-005-006-013.md.
    @Test
    void unverifiedBrowsingAndSearchApisAreBlockedTooNotJustPostingAndMessaging() throws Exception {
        authenticatedUnverifiedUser();
        MockHttpServletResponse listResponse = new MockHttpServletResponse();
        MockHttpServletResponse pageResponse = new MockHttpServletResponse();

        filter.doFilter(request("/api/posts/page"), listResponse, mock(FilterChain.class));
        filter.doFilter(request("/posts"), pageResponse, mock(FilterChain.class));

        assertEquals(403, listResponse.getStatus());
        assertTrue(listResponse.getContentAsString().contains("EMAIL_NOT_VERIFIED"));
        assertEquals("/verify-email", pageResponse.getRedirectedUrl());
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
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(path);
        return request;
    }

    private void authenticatedUnverifiedUser() {
        User user = new User("Member", "member@example.com", "hash");
        when(users.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("member@example.com", null,
                        List.of()));
    }
}
