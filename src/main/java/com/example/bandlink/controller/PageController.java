package com.example.bandlink.controller;

import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PageController {
    private final String baseUrl;
    private final String googleAnalyticsId;

    public PageController(@Value("${app.base-url:http://localhost:8080}") String baseUrl,
                           @Value("${app.google-analytics-id:}") String googleAnalyticsId) {
        this.baseUrl = baseUrl.replaceFirst("/+?$", "");
        this.googleAnalyticsId = googleAnalyticsId;
    }

    @GetMapping({"/", "/posts", "/posts/new", "/posts/{id}", "/posts/{id}/edit",
            "/my/posts", "/users/{id}", "/settings", "/settings/profile", "/settings/blocks",
            "/login", "/register", "/verify-email", "/password-reset", "/password-reset/confirm",
            "/messages", "/messages/{id}", "/notifications", "/blocks", "/admin", "/admin/reports", "/support",
            "/contact", "/feature-request"})
    public String app(HttpServletRequest request, Model model) {
        String path = request.getRequestURI();
        boolean publicPage = "/".equals(path) || "/posts".equals(path)
                || path.matches("/posts/[0-9]+") || path.matches("/users/[0-9]+");
        String canonicalPath = "/".equals(path) ? "/posts" : path;
        model.addAttribute("seoRobots", publicPage ? "index,follow" : "noindex,nofollow");
        model.addAttribute("seoCanonical", baseUrl + canonicalPath);
        model.addAttribute("seoOgUrl", baseUrl + canonicalPath);
        model.addAttribute("googleAnalyticsId", googleAnalyticsId);
        return "posts";
    }

    @GetMapping("/api/csrf")
    @ResponseBody
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }
}
