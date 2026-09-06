package com.example.bandlink.config;

import org.springframework.context.annotation.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(org.springframework.security.config.Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/me", "/api/auth/logout", "/api/auth/withdraw", "/api/users/me", "/api/posts/mine").authenticated()
                .requestMatchers("/api/admin/**", "/admin").hasRole("ADMIN")
                .requestMatchers("/api/auth/**", "/api/csrf", "/api/masters", "/login", "/register",
                    "/verify-email", "/password-reset", "/password-reset/confirm", "/css/**", "/js/**", "/assets/**", "/uploads/**",
                    "/", "/posts", "/support", "/error").permitAll()
                .requestMatchers("/posts/new", "/posts/*/edit").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/posts/*", "/users/*").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/posts/**", "/api/users/*").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.setStatus(401);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"ログインしてください。\"}");
                    } else {
                        response.sendRedirect("/login?next=" + java.net.URLEncoder.encode(request.getRequestURI(), java.nio.charset.StandardCharsets.UTF_8));
                    }
                })
                .accessDeniedHandler((request, response, exception) -> {
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"この操作は許可されていません。ページを再読み込みしてご確認ください。\"}");
                }))
            .formLogin(form -> form.loginPage("/login").loginProcessingUrl("/login").usernameParameter("email").defaultSuccessUrl("/", true).failureUrl("/login?error"))
            .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login"));
        return http.build();
    }
}
