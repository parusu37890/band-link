package com.example.bandlink.config;

import org.springframework.context.annotation.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import com.example.bandlink.repository.UserRepository;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean EmailVerificationGateFilter emailVerificationGateFilter(UserRepository users) {
        return new EmailVerificationGateFilter(users);
    }

    @Bean LastSeenFilter lastSeenFilter(UserRepository users) {
        return new LastSeenFilter(users);
    }

    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   EmailVerificationGateFilter emailVerificationGateFilter,
                                                   LastSeenFilter lastSeenFilter) throws Exception {
        http
            .csrf(org.springframework.security.config.Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                // The private message-image and feedback-attachment directories sit inside the same physical tree that
                // /uploads/** maps to below (StaticResourceConfig has no way to carve a subpath
                // out of a WebMvc resource handler), so it must be refused here, ahead of that
                // permitAll, or the checks on /api/messages/images/{name} and
                // /api/admin/feedback/images/{name} are bypassed by alternate public URLs.
                // ImageStorageService returns the guarded /api URLs for both private directories.
                .requestMatchers("/uploads/messages/**", "/uploads/feedback/**").denyAll()
                .requestMatchers("/api/auth/me", "/api/auth/logout", "/api/auth/withdraw", "/api/users/me", "/api/posts/mine").authenticated()
                .requestMatchers("/api/admin/**", "/admin").hasRole("ADMIN")
                .requestMatchers("/api/auth/**", "/api/csrf", "/api/masters", "/login", "/register",
                    "/verify-email", "/password-reset", "/password-reset/confirm", "/css/**", "/js/**", "/assets/**", "/uploads/**",
                    "/", "/posts", "/support", "/contact", "/feature-request", "/robots.txt", "/sitemap.xml", "/error").permitAll()
                .requestMatchers("/posts/new", "/posts/*/edit").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/posts/*", "/users/*").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/posts/**", "/api/users/*").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) -> {
                    String uri = request.getRequestURI();
                    if (uri.startsWith("/api/")) {
                        response.setStatus(401);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"ログインしてください。\"}");
                    } else if (uri.startsWith("/uploads/")) {
                        // Private /uploads subpaths are denied to everyone, not gated on being
                        // signed in (see the denyAll above), so redirecting to /login here would offer
                        // a fix that does not exist. An <img> tag cannot follow a redirect to an HTML
                        // page either way; a flat status is both more honest and what the tag needs.
                        response.setStatus(403);
                    } else {
                        response.sendRedirect("/login?next=" + java.net.URLEncoder.encode(request.getRequestURI(), java.nio.charset.StandardCharsets.UTF_8));
                    }
                })
                .accessDeniedHandler((request, response, exception) -> {
                    // Mirrors the authenticationEntryPoint branch above: /admin carries the same
                    // hasRole("ADMIN") rule as /api/admin/**, so a signed-in non-admin who follows a
                    // stale link or types the URL hit this same handler as any rejected API call and
                    // got the raw {"code":"FORBIDDEN",...} JSON printed as plain text on a blank white
                    // page - no header, no footer, none of the page canvas ST-055 checks every route
                    // for. A page request gets sent somewhere it can actually land instead.
                    String uri = request.getRequestURI();
                    if (uri.startsWith("/api/")) {
                        response.setStatus(403);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"この操作は許可されていません。ページを再読み込みしてご確認ください。\"}");
                    } else {
                        response.sendRedirect("/posts");
                    }
                }))
            .formLogin(form -> form.loginPage("/login").loginProcessingUrl("/login").usernameParameter("email").defaultSuccessUrl("/", true).failureUrl("/login?error"))
            .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login"));
        http.addFilterBefore(emailVerificationGateFilter, AuthorizationFilter.class);
        // After the gate: an account still stuck on the verification screen is not "here" on the board.
        http.addFilterAfter(lastSeenFilter, AuthorizationFilter.class);
        return http.build();
    }
}
