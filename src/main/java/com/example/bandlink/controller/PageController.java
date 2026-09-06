package com.example.bandlink.controller;

import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PageController {
    @GetMapping({"/", "/posts", "/posts/new", "/posts/{id}", "/posts/{id}/edit",
            "/my/posts", "/users/{id}", "/settings", "/settings/profile", "/settings/blocks",
            "/login", "/register", "/verify-email", "/password-reset", "/password-reset/confirm",
            "/messages", "/messages/{id}", "/notifications", "/blocks", "/admin", "/admin/reports", "/support"})
    public String app() {
        return "posts";
    }

    @GetMapping("/api/csrf")
    @ResponseBody
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }
}
