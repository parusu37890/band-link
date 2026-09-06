package com.example.bandlink.controller;

import com.example.bandlink.dto.RegisterRequest;
import com.example.bandlink.dto.UserResponse;
import com.example.bandlink.dto.TokenRequests.*;
import com.example.bandlink.entity.User;
import com.example.bandlink.repository.UserRepository;
import com.example.bandlink.service.AuthService;
import com.example.bandlink.service.LineLoginService;
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
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(AuthService authService, AuthenticationManager authenticationManager, UserRepository userRepository,
                          LineLoginService lineLogin) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.lineLogin = lineLogin;
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
        Authentication authentication = startSession(request.email(), request.password(), httpRequest, httpResponse);
        touchLogin(request.email());
        return currentUser(authentication);
    }

    /** Establishes the session both entry points need; the session id is rotated on the way in. */
    private Authentication startSession(String email, String password,
                                        HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, password));
        if (httpRequest.getSession(false) != null) httpRequest.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        return authentication;
    }

    private void startLineSession(User user, HttpServletRequest request, HttpServletResponse response) {
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
            startLineSession(user, request, response);
            response.sendRedirect(user.getStatus() == com.example.bandlink.entity.UserStatus.SUSPENDED
                    ? "/support" : user.isEmailVerified() ? "/posts" : "/verify-email");
        } catch (LineLoginService.LineLoginException e) {
            response.sendRedirect("/login?lineError=failed");
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request.token());
        return ResponseEntity.noContent().build();
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
