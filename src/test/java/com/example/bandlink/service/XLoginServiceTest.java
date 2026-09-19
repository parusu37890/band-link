package com.example.bandlink.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.bandlink.entity.User;
import com.example.bandlink.repository.UserRepository;
import com.example.bandlink.repository.XAccountRepository;
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

class XLoginServiceTest {
    private XLoginService service(String id, String secret, String redirect) {
        return new XLoginService(id, secret, redirect, "http://localhost:8080",
                mock(UserRepository.class), mock(XAccountRepository.class), mock(PasswordEncoder.class));
    }

    private record Fixture(XLoginService service, MockRestServiceServer server,
                           UserRepository users, XAccountRepository accounts, PasswordEncoder encoder) {}

    private Fixture apiFixture() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UserRepository users = mock(UserRepository.class);
        XAccountRepository accounts = mock(XAccountRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        return new Fixture(new XLoginService("client", "secret", "http://localhost:8080/callback",
                "http://localhost:8080", users, accounts, encoder, builder.build()), server, users, accounts, encoder);
    }

    @Test
    void authorizationUrlContainsPkceStateAndCallback() {
        XLoginService x = service("client id", "secret", "http://localhost:8080/api/auth/x/callback");

        String verifier = x.newCodeVerifier();
        String url = x.authorizationUrl("csrf-state", verifier);

        assertTrue(x.enabled());
        assertTrue(url.startsWith("https://x.com/i/oauth2/authorize?"));
        assertTrue(url.contains("client_id=client+id"));
        assertTrue(url.contains("redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fapi%2Fauth%2Fx%2Fcallback"));
        assertTrue(url.contains("state=csrf-state"));
        assertTrue(url.contains("scope=users.read"));
        assertFalse(url.contains("tweet.read"));
        assertTrue(url.contains("code_challenge="));
        assertTrue(url.contains("code_challenge_method=S256"));
    }

    @Test
    void missingCredentialsDisableXLogin() {
        XLoginService x = service("", "", "");

        assertFalse(x.enabled());
        assertEquals("http://localhost:8080/api/auth/x/callback", x.redirectUri());
    }

    @Test
    void exchangesCodeAndCreatesLocalAccountFromMockedXApi() {
        Fixture f = apiFixture();
        when(f.accounts.findByXUserId("123")).thenReturn(java.util.Optional.empty());
        when(f.users.existsByEmail(any())).thenReturn(false);
        when(f.encoder.encode(any())).thenReturn("encoded-password");
        when(f.users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        f.server.expect(requestTo("https://api.x.com/2/oauth2/token"))
                .andExpect(method(POST))
                .andRespond(withSuccess("{\"access_token\":\"access-token\"}", MediaType.APPLICATION_JSON));
        f.server.expect(requestTo("https://api.x.com/2/users/me"))
                .andExpect(method(GET))
                .andRespond(withSuccess("{\"data\":{\"id\":\"123\",\"name\":\"ライブ仲間\",\"username\":\"live_nakama\"}}", MediaType.APPLICATION_JSON));

        User user = f.service.login("one-time-code", "verifier");

        assertEquals("ライブ仲間", user.getUsername());
        assertTrue(user.isEmailVerified());
        verify(f.accounts).save(argThat(account -> "123".equals(account.getXUserId())));
        f.server.verify();
    }

    @Test
    void rejectsMissingAccessTokenFromMockedXApi() {
        Fixture f = apiFixture();
        f.server.expect(requestTo("https://api.x.com/2/oauth2/token"))
                .andExpect(method(POST))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        XLoginService.XLoginException error = assertThrows(XLoginService.XLoginException.class,
                () -> f.service.login("one-time-code", "verifier"));

        assertEquals("Xからアクセストークンを受け取れませんでした", error.getMessage());
        f.server.verify();
    }
}
