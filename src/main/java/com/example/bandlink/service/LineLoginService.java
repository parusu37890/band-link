package com.example.bandlink.service;

import com.example.bandlink.entity.LineAccount;
import com.example.bandlink.entity.User;
import com.example.bandlink.repository.LineAccountRepository;
import com.example.bandlink.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** LINE Login v2.1 authorization-code flow and local account mapping. */
@Service
public class LineLoginService {
    private static final String AUTHORIZE_ENDPOINT = "https://access.line.me/oauth2/v2.1/authorize";
    private static final String TOKEN_ENDPOINT = "https://api.line.me/oauth2/v2.1/token";
    private static final String PROFILE_ENDPOINT = "https://api.line.me/v2/profile";
    private final RestClient client = RestClient.create();
    private final String channelId;
    private final String channelSecret;
    private final String redirectUri;
    private final UserRepository users;
    private final LineAccountRepository lineAccounts;
    private final PasswordEncoder passwordEncoder;

    public LineLoginService(
            @Value("${app.line.channel-id:}") String channelId,
            @Value("${app.line.channel-secret:}") String channelSecret,
            @Value("${app.line.redirect-uri:}") String redirectUri,
            @Value("${app.base-url:http://localhost:8080}") String baseUrl,
            UserRepository users, LineAccountRepository lineAccounts, PasswordEncoder passwordEncoder) {
        this.channelId = channelId == null ? "" : channelId.trim();
        this.channelSecret = channelSecret == null ? "" : channelSecret.trim();
        this.redirectUri = redirectUri == null || redirectUri.isBlank()
                ? baseUrl.replaceAll("/$", "") + "/api/auth/line/callback"
                : redirectUri.trim();
        this.users = users;
        this.lineAccounts = lineAccounts;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean enabled() {
        return !channelId.isBlank() && !channelSecret.isBlank() && !redirectUri.isBlank();
    }

    public String authorizationUrl(String state) {
        if (!enabled()) throw new LineLoginException("LINEログインはまだ設定されていません");
        return AUTHORIZE_ENDPOINT
                + "?response_type=code"
                + "&client_id=" + encode(channelId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&state=" + encode(state)
                + "&scope=profile%20openid";
    }

    @Transactional
    public User login(String code) {
        if (!enabled()) throw new LineLoginException("LINEログインはまだ設定されていません");
        if (code == null || code.isBlank()) throw new LineLoginException("LINEログインの認証コードを受け取れませんでした");
        try {
            LineToken token = exchangeCode(code);
            LineProfile profile = fetchProfile(token.accessToken());
            return lineAccounts.findByLineUserId(profile.userId())
                    .map(LineAccount::getUser)
                    .orElseGet(() -> createAccount(profile));
        } catch (RestClientException e) {
            throw new LineLoginException("LINEとの通信に失敗しました", e);
        }
    }

    private LineToken exchangeCode(String code) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri);
        form.add("client_id", channelId);
        form.add("client_secret", channelSecret);
        LineToken token = client.post().uri(TOKEN_ENDPOINT)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(LineToken.class);
        if (token == null || token.accessToken() == null || token.accessToken().isBlank())
            throw new LineLoginException("LINEからアクセストークンを受け取れませんでした");
        return token;
    }

    private LineProfile fetchProfile(String accessToken) {
        LineProfile profile = client.get().uri(PROFILE_ENDPOINT)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(LineProfile.class);
        if (profile == null || profile.userId() == null || profile.userId().isBlank())
            throw new LineLoginException("LINEのユーザー情報を受け取れませんでした");
        return profile;
    }

    private User createAccount(LineProfile profile) {
        String email = syntheticEmail(profile.userId());
        if (users.existsByEmail(email)) throw new LineLoginException("LINEアカウントの紐付けを確認できませんでした");
        String username = profile.displayName() == null || profile.displayName().isBlank()
                ? "LINEユーザー" : profile.displayName().trim();
        if (username.length() > 80) username = username.substring(0, 80);
        User user = new User(username, email, passwordEncoder.encode(UUID.randomUUID().toString()));
        // LINE has already authenticated the account. No local email is stored or exposed.
        user.setEmailVerifiedAt(LocalDateTime.now());
        user = users.save(user);
        lineAccounts.save(new LineAccount(user, profile.userId()));
        return user;
    }

    private String syntheticEmail(String lineUserId) {
        String stableId = UUID.nameUUIDFromBytes(lineUserId.getBytes(StandardCharsets.UTF_8))
                .toString().replace("-", "");
        return "line-" + stableId + "@accounts.bandlink.local";
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public String redirectUri() { return redirectUri; }

    public record LineToken(@com.fasterxml.jackson.annotation.JsonProperty("access_token") String accessToken) {}
    public record LineProfile(@com.fasterxml.jackson.annotation.JsonProperty("userId") String userId,
                              @com.fasterxml.jackson.annotation.JsonProperty("displayName") String displayName) {}

    public static class LineLoginException extends RuntimeException {
        public LineLoginException(String message) { super(message); }
        public LineLoginException(String message, Throwable cause) { super(message, cause); }
    }
}
