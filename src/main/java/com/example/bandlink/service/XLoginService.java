package com.example.bandlink.service;

import com.example.bandlink.entity.User;
import com.example.bandlink.entity.XAccount;
import com.example.bandlink.repository.UserRepository;
import com.example.bandlink.repository.XAccountRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.dao.DataIntegrityViolationException;

/** X (Twitter) OAuth 2.0 authorization-code + PKCE flow and local account mapping. */
@Service
public class XLoginService {
    private static final String AUTHORIZE_ENDPOINT = "https://x.com/i/oauth2/authorize";
    private static final String TOKEN_ENDPOINT = "https://api.x.com/2/oauth2/token";
    private static final String PROFILE_ENDPOINT = "https://api.x.com/2/users/me";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final RestClient client;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final UserRepository users;
    private final XAccountRepository xAccounts;
    private final PasswordEncoder passwordEncoder;

    // Mirrors LineLoginService's two-constructor split: the primary one Spring wires, the
    // package-private one a MockRestServiceServer-backed test swaps the RestClient into.
    @org.springframework.beans.factory.annotation.Autowired
    public XLoginService(
            @Value("${app.x.client-id:}") String clientId,
            @Value("${app.x.client-secret:}") String clientSecret,
            @Value("${app.x.redirect-uri:}") String redirectUri,
            @Value("${app.base-url:http://localhost:8080}") String baseUrl,
            UserRepository users, XAccountRepository xAccounts, PasswordEncoder passwordEncoder) {
        this(clientId, clientSecret, redirectUri, baseUrl, users, xAccounts, passwordEncoder, RestClient.create());
    }

    /** Package-private seam for MockRestServiceServer-backed unit tests. */
    XLoginService(String clientId, String clientSecret, String redirectUri, String baseUrl,
                 UserRepository users, XAccountRepository xAccounts, PasswordEncoder passwordEncoder,
                 RestClient client) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.clientSecret = clientSecret == null ? "" : clientSecret.trim();
        this.redirectUri = redirectUri == null || redirectUri.isBlank()
                ? baseUrl.replaceAll("/$", "") + "/api/auth/x/callback"
                : redirectUri.trim();
        this.users = users;
        this.xAccounts = xAccounts;
        this.passwordEncoder = passwordEncoder;
        this.client = client == null ? RestClient.create() : client;
    }

    public boolean enabled() {
        return !clientId.isBlank() && !clientSecret.isBlank() && !redirectUri.isBlank();
    }

    /** A fresh PKCE code_verifier, kept in the session alongside the CSRF state until the callback. */
    public String newCodeVerifier() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String authorizationUrl(String state, String codeVerifier) {
        if (!enabled()) throw new XLoginException("Xログインはまだ設定されていません");
        return AUTHORIZE_ENDPOINT
                + "?response_type=code"
                + "&client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&state=" + encode(state)
                + "&scope=" + encode("users.read tweet.read")
                + "&code_challenge=" + encode(codeChallenge(codeVerifier))
                + "&code_challenge_method=S256";
    }

    @Transactional
    public User login(String code, String codeVerifier) {
        if (!enabled()) throw new XLoginException("Xログインはまだ設定されていません");
        if (code == null || code.isBlank()) throw new XLoginException("Xログインの認証コードを受け取れませんでした");
        try {
            XToken token = exchangeCode(code, codeVerifier);
            XProfile profile = fetchProfile(token.accessToken());
            String xUserId = profile.data().id().trim();
            return xAccounts.findByXUserId(xUserId)
                    .map(XAccount::getUser)
                    .orElseGet(() -> createAccount(profile, xUserId));
        } catch (RestClientException e) {
            throw new XLoginException("Xとの通信に失敗しました", e);
        } catch (DataIntegrityViolationException e) {
            // A second callback can race the first one. The unique X id constraint wins;
            // expose a safe retry message instead of a database error.
            throw new XLoginException("Xアカウントの登録が競合しました。もう一度お試しください", e);
        }
    }

    private XToken exchangeCode(String code, String codeVerifier) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri);
        form.add("client_id", clientId);
        form.add("code_verifier", codeVerifier);
        XToken token = client.post().uri(TOKEN_ENDPOINT)
                .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(XToken.class);
        if (token == null || token.accessToken() == null || token.accessToken().isBlank())
            throw new XLoginException("Xからアクセストークンを受け取れませんでした");
        return token;
    }

    private XProfile fetchProfile(String accessToken) {
        XProfile profile = client.get().uri(PROFILE_ENDPOINT)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(XProfile.class);
        if (profile == null || profile.data() == null || profile.data().id() == null || profile.data().id().isBlank())
            throw new XLoginException("Xのユーザー情報を受け取れませんでした");
        return profile;
    }

    private User createAccount(XProfile profile, String xUserId) {
        String email = syntheticEmail(xUserId);
        if (users.existsByEmail(email)) throw new XLoginException("Xアカウントの紐付けを確認できませんでした");
        String name = profile.data().name();
        String username = name == null || name.isBlank() ? "Xユーザー" : name.trim();
        if (username.length() > 80) username = username.substring(0, 80);
        User user = new User(username, email, passwordEncoder.encode(UUID.randomUUID().toString()));
        // X has already authenticated the account. No local email is stored or exposed.
        user.setEmailVerifiedAt(LocalDateTime.now());
        user = users.save(user);
        xAccounts.save(new XAccount(user, xUserId));
        return user;
    }

    private String syntheticEmail(String xUserId) {
        String stableId = UUID.nameUUIDFromBytes(xUserId.getBytes(StandardCharsets.UTF_8))
                .toString().replace("-", "");
        return "x-" + stableId + "@accounts.bandlink.local";
    }

    private String codeChallenge(String codeVerifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public String redirectUri() { return redirectUri; }

    public record XToken(@com.fasterxml.jackson.annotation.JsonProperty("access_token") String accessToken) {}
    public record XProfile(@com.fasterxml.jackson.annotation.JsonProperty("data") XProfileData data) {}
    public record XProfileData(@com.fasterxml.jackson.annotation.JsonProperty("id") String id,
                               @com.fasterxml.jackson.annotation.JsonProperty("name") String name,
                               @com.fasterxml.jackson.annotation.JsonProperty("username") String username) {}

    public static class XLoginException extends RuntimeException {
        public XLoginException(String message) { super(message); }
        public XLoginException(String message, Throwable cause) { super(message, cause); }
    }
}
