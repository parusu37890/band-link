package com.example.bandlink.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.example.bandlink.repository.LineAccountRepository;
import com.example.bandlink.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class LineLoginServiceTest {
    private LineLoginService service(String id, String secret, String redirect) {
        return new LineLoginService(id, secret, redirect, "http://localhost:8080",
                mock(UserRepository.class), mock(LineAccountRepository.class), mock(PasswordEncoder.class));
    }

    @Test
    void authorizationUrlContainsOAuthStateAndCallback() {
        LineLoginService line = service("channel id", "secret", "http://localhost:8080/api/auth/line/callback");

        String url = line.authorizationUrl("csrf-state");

        assertTrue(line.enabled());
        assertTrue(url.startsWith("https://access.line.me/oauth2/v2.1/authorize?"));
        assertTrue(url.contains("client_id=channel+id"));
        assertTrue(url.contains("redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fapi%2Fauth%2Fline%2Fcallback"));
        assertTrue(url.contains("state=csrf-state"));
        assertTrue(url.contains("scope=profile%20openid"));
    }

    @Test
    void missingCredentialsDisableLineLogin() {
        LineLoginService line = service("", "", "");

        assertFalse(line.enabled());
        assertEquals("http://localhost:8080/api/auth/line/callback", line.redirectUri());
    }
}
