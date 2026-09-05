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
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/login", "/register", "/css/**", "/uploads/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/posts/**", "/api/users/*").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form.loginProcessingUrl("/login").usernameParameter("email").defaultSuccessUrl("/", true).failureUrl("/login?error"))
            .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login"));
        return http.build();
    }
}
