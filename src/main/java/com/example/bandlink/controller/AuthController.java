package com.example.bandlink.controller;

import com.example.bandlink.dto.RegisterRequest;
import com.example.bandlink.dto.UserResponse;
import com.example.bandlink.dto.TokenRequests.*;
import com.example.bandlink.entity.User;
import com.example.bandlink.repository.UserRepository;
import com.example.bandlink.service.AuthService;
import com.example.bandlink.service.LineLoginService;
import com.example.bandlink.service.LoginAttemptService;
import com.example.bandlink.service.XLoginService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final LineLoginService lineLogin;
    private final XLoginService xLogin;
    private final LoginAttemptService loginAttempts;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(AuthService authService, AuthenticationManager authenticationManager, UserRepository userRepository,
                          LineLoginService lineLogin, XLoginService xLogin, LoginAttemptService loginAttempts) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.lineLogin = lineLogin;
        this.xLogin = xLogin;
        this.loginAttempts = loginAttempts;
    }

    /**
     * Registration signs the person in. The client sets state.user from this response and sends
     * them into the app, so without a session that was a lie: found by ST on 2026-09-06, a new
     * account landed on the board anonymous, told neither to log in nor to verify the address it
     * had just taken. Verification still gates posting and messaging — being signed in is what
     * lets the app say so.
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.register(request);
        Authentication authentication = startSession(request.email(), request.password(), httpRequest, httpResponse);
        touchLogin(request.email());
        return ResponseEntity.status(201).body(currentUser(authentication));
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // Do not pre-check whether the email exists: that turns a wrong-password reply
        // ("メールアドレスとパスワードを確認してください。") and an unknown-email reply into a
        // registered-email oracle. BandLinkUserDetailsService already throws
        // UsernameNotFoundException for a missing user, which Spring Security's default
        // hideUserNotFoundExceptions=true converts to the same BadCredentialsException as a
        // wrong password, so both land on ApiExceptionHandler's single generic message.
        //
        // SEC-013: found via Playwright MCP that this endpoint had no brute-force protection at
        // all - repeated wrong-password attempts against the same account all came back
        // identically with no delay or lockout. loginAttempts is keyed by the raw email attempted
        // (not by whether it resolves to a real account), so this lockout check adds no signal
        // beyond the comment above: a nonexistent address locks out on the same schedule as a
        // real one.
        if (loginAttempts.isLocked(request.email())) throw new AuthService.TooManyAttemptsException();
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        } catch (org.springframework.security.core.AuthenticationException e) {
            loginAttempts.recordFailure(request.email());
            throw e;
        }
        loginAttempts.recordSuccess(request.email());
        // An unverified account signs in the same as any other - EmailVerificationGateFilter is
        // what keeps it off everything but the verify-email screen and read-only browsing, and the
        // client sends it straight to /verify-email on seeing emailVerified:false in the response.
        establishSession(authentication, httpRequest, httpResponse);
        touchLogin(request.email());
        return currentUser(authentication);
    }

    /** Establishes the session both entry points need; the session id is rotated on the way in. */
    private Authentication startSession(String email, String password,
                                        HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, password));
        establishSession(authentication, httpRequest, httpResponse);
        return authentication;
    }

    private void establishSession(Authentication authentication, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        if (httpRequest.getSession(false) != null) httpRequest.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
    }

    /** Shared by both LINE and X callbacks - either one hands off an already-authenticated user. */
    private void startExternalSession(User user, HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) != null) request.changeSessionId();
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                user.getEmail(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        user.touchLogin(java.time.LocalDateTime.now());
        userRepository.save(user);
    }

    private void touchLogin(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.touchLogin(java.time.LocalDateTime.now());
            userRepository.save(user);
        });
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) { return currentUser(authentication); }

    @GetMapping("/line/enabled")
    public Map<String, Boolean> lineEnabled() { return Map.of("enabled", lineLogin.enabled()); }

    @GetMapping("/line/start")
    public void startLineLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!lineLogin.enabled()) {
            response.sendRedirect("/login?lineError=unavailable");
            return;
        }
        String state = UUID.randomUUID().toString();
        request.getSession(true).setAttribute("BANDLINK_LINE_STATE", state);
        response.sendRedirect(lineLogin.authorizationUrl(state));
    }

    @GetMapping("/line/callback")
    public void lineCallback(@RequestParam(required = false) String code,
                             @RequestParam(required = false) String state,
                             @RequestParam(required = false) String error,
                             HttpServletRequest request, HttpServletResponse response) throws IOException {
        var session = request.getSession(false);
        Object expectedValue = session == null ? null : session.getAttribute("BANDLINK_LINE_STATE");
        if (session != null) session.removeAttribute("BANDLINK_LINE_STATE");
        if (error != null || code == null || expectedValue == null || state == null
                || !MessageDigest.isEqual(String.valueOf(expectedValue).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                                           state.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            response.sendRedirect("/login?lineError=cancelled");
            return;
        }
        try {
            User user = lineLogin.login(code);
            startExternalSession(user, request, response);
            response.sendRedirect(user.getStatus() == com.example.bandlink.entity.UserStatus.SUSPENDED
                    ? "/support" : user.isEmailVerified() ? (profileComplete(user) ? "/posts" : "/settings/profile") : "/verify-email");
        } catch (LineLoginService.LineLoginException e) {
            response.sendRedirect("/login?lineError=failed");
        }
    }

    @GetMapping("/x/enabled")
    public Map<String, Boolean> xEnabled() { return Map.of("enabled", xLogin.enabled()); }

    @GetMapping("/x/start")
    public void startXLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!xLogin.enabled()) {
            response.sendRedirect("/login?xError=unavailable");
            return;
        }
        String state = UUID.randomUUID().toString();
        String codeVerifier = xLogin.newCodeVerifier();
        var session = request.getSession(true);
        session.setAttribute("BANDLINK_X_STATE", state);
        session.setAttribute("BANDLINK_X_VERIFIER", codeVerifier);
        response.sendRedirect(xLogin.authorizationUrl(state, codeVerifier));
    }

    @GetMapping("/x/callback")
    public void xCallback(@RequestParam(required = false) String code,
                          @RequestParam(required = false) String state,
                          @RequestParam(required = false) String error,
                          HttpServletRequest request, HttpServletResponse response) throws IOException {
        var session = request.getSession(false);
        Object expectedState = session == null ? null : session.getAttribute("BANDLINK_X_STATE");
        Object codeVerifier = session == null ? null : session.getAttribute("BANDLINK_X_VERIFIER");
        if (session != null) {
            session.removeAttribute("BANDLINK_X_STATE");
            session.removeAttribute("BANDLINK_X_VERIFIER");
        }
        if (error != null || code == null || expectedState == null || codeVerifier == null || state == null
                || !MessageDigest.isEqual(String.valueOf(expectedState).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                                           state.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            response.sendRedirect("/login?xError=cancelled");
            return;
        }
        try {
            User user = xLogin.login(code, String.valueOf(codeVerifier));
            startExternalSession(user, request, response);
            response.sendRedirect(user.getStatus() == com.example.bandlink.entity.UserStatus.SUSPENDED
                    ? "/support" : user.isEmailVerified() ? (profileComplete(user) ? "/posts" : "/settings/profile") : "/verify-email");
        } catch (XLoginService.XLoginException e) {
            response.sendRedirect("/login?xError=failed");
        }
    }

    private boolean profileComplete(User user) {
        return user.getAge() != null && user.getExperienceYears() != null
                && ("男性".equals(user.getGender()) || "女性".equals(user.getGender()))
                && !user.getParts().isEmpty() && !user.getGenres().isEmpty()
                && !user.getStances().isEmpty() && !user.getPrefectures().isEmpty();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email/resend")
    public ResponseEntity<Void> resendVerification(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("認証ユーザーが見つかりません"));
        authService.resendVerification(user.getId());
        return ResponseEntity.accepted().build();
    }

    /**
     * Login now rejects an unverified account outright, so someone who lost or outlived their
     * verification email has no session to ask for a new one from. Reachable without one, by email,
     * like password-reset/request below - always 202, verified or not, real address or not.
     */
    @PostMapping("/verify-email/resend-request")
    public ResponseEntity<Void> resendVerificationByEmail(@Valid @RequestBody VerifyEmailResendRequest request) {
        authService.resendVerificationByEmail(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.confirmPasswordReset(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Void> withdraw(Authentication authentication, HttpServletRequest request) {
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new IllegalStateException("認証ユーザーが見つかりません"));
        authService.withdraw(user.getId());
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) request.getSession(false).invalidate();
        return ResponseEntity.noContent().build();
    }

    private UserResponse currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "ログインが必要です");
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("認証ユーザーが見つかりません"));
        return UserResponse.from(user);
    }

    public record LoginRequest(@jakarta.validation.constraints.NotBlank String email,
                               @jakarta.validation.constraints.NotBlank String password) {}
}
