package com.example.bandlink.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.bandlink.entity.User;
import com.example.bandlink.repository.LineAccountRepository;
import com.example.bandlink.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

class LineLoginServiceTest {
    private LineLoginService service(String id, String secret, String redirect) {
        return new LineLoginService(id, secret, redirect, "http://localhost:8080",
                mock(UserRepository.class), mock(LineAccountRepository.class), mock(PasswordEncoder.class));
    }

    private record Fixture(LineLoginService service, MockRestServiceServer server,
                           UserRepository users, LineAccountRepository accounts, PasswordEncoder encoder) {}

    private Fixture apiFixture() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UserRepository users = mock(UserRepository.class);
        LineAccountRepository accounts = mock(LineAccountRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        return new Fixture(new LineLoginService("channel", "secret", "http://localhost:8080/callback",
                "http://localhost:8080", users, accounts, encoder, builder.build()), server, users, accounts, encoder);
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

    @Test
    void exchangesCodeAndCreatesLocalAccountFromMockedLineApi() {
        Fixture f = apiFixture();
        when(f.accounts.findByLineUserId("U123")).thenReturn(java.util.Optional.empty());
        when(f.users.existsByEmail(any())).thenReturn(false);
        when(f.encoder.encode(any())).thenReturn("encoded-password");
        when(f.users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        f.server.expect(requestTo("https://api.line.me/oauth2/v2.1/token"))
                .andExpect(method(POST))
                .andRespond(withSuccess("{\"access_token\":\"access-token\"}", MediaType.APPLICATION_JSON));
        f.server.expect(requestTo("https://api.line.me/v2/profile"))
                .andExpect(method(GET))
                .andRespond(withSuccess("{\"userId\":\"U123\",\"displayName\":\"ライブ仲間\"}", MediaType.APPLICATION_JSON));

        User user = f.service.login("one-time-code");

        assertEquals("ライブ仲間", user.getUsername());
        assertTrue(user.isEmailVerified());
        verify(f.accounts).save(argThat(account -> "U123".equals(account.getLineUserId())));
        f.server.verify();
    }

    @Test
    void rejectsMissingAccessTokenFromMockedLineApi() {
        Fixture f = apiFixture();
        f.server.expect(requestTo("https://api.line.me/oauth2/v2.1/token"))
                .andExpect(method(POST))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        LineLoginService.LineLoginException error = assertThrows(LineLoginService.LineLoginException.class,
                () -> f.service.login("one-time-code"));

        assertEquals("LINEからアクセストークンを受け取れませんでした", error.getMessage());
        f.server.verify();
    }
}
