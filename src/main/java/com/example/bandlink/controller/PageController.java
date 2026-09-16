package com.example.bandlink.controller;

import com.example.bandlink.entity.Post;
import com.example.bandlink.entity.PostStatus;
import com.example.bandlink.entity.User;
import com.example.bandlink.entity.UserStatus;
import com.example.bandlink.repository.PostRepository;
import com.example.bandlink.repository.UserRepository;
import java.util.Map;
import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PageController {
    private static final String DEFAULT_TITLE = "Band Link バンドメンバー募集・加入サイト";
    private static final String DEFAULT_DESCRIPTION =
            "バンドメンバー募集・加入希望の掲示板です。2026/09/15にリリースしました！閲覧は登録不要、メッセージのやり取りは登録後に無料で使えます。";

    private final String baseUrl;
    private final String googleAnalyticsId;
    private final PostRepository posts;
    private final UserRepository users;

    public PageController(@Value("${app.base-url:http://localhost:8080}") String baseUrl,
                           @Value("${app.google-analytics-id:}") String googleAnalyticsId,
                           PostRepository posts, UserRepository users) {
        this.baseUrl = baseUrl.replaceFirst("/+?$", "");
        this.googleAnalyticsId = googleAnalyticsId;
        this.posts = posts;
        this.users = users;
    }

    @GetMapping({"/", "/posts", "/posts/new", "/posts/{id}", "/posts/{id}/edit",
            "/my/posts", "/users/{id}", "/settings", "/settings/profile", "/settings/blocks",
            "/login", "/register", "/verify-email", "/password-reset", "/password-reset/confirm",
            "/messages", "/messages/{id}", "/notifications", "/blocks", "/admin", "/admin/reports", "/support",
            "/contact", "/feature-request", "/privacy"})
    public String app(HttpServletRequest request, Model model) {
        String path = request.getRequestURI();
        boolean publicPage = "/".equals(path) || "/posts".equals(path)
                || path.matches("/posts/[0-9]+") || path.matches("/users/[0-9]+");
        String canonicalPath = "/".equals(path) ? "/posts" : path;
        model.addAttribute("seoRobots", publicPage ? "index,follow" : "noindex,nofollow");
        model.addAttribute("seoCanonical", baseUrl + canonicalPath);
        model.addAttribute("seoOgUrl", baseUrl + canonicalPath);
        model.addAttribute("googleAnalyticsId", googleAnalyticsId);

        String title = DEFAULT_TITLE;
        String description = DEFAULT_DESCRIPTION;
        if (path.matches("/posts/[0-9]+")) {
            Optional<Post> post = posts.findById(Long.valueOf(path.substring("/posts/".length())))
                    .filter(p -> p.getStatus() == PostStatus.OPEN);
            if (post.isPresent()) {
                title = post.get().getTitle() + " — Band Link";
                description = summarize(post.get().getContent());
            }
        } else if (path.matches("/users/[0-9]+")) {
            Optional<User> user = users.findById(Long.valueOf(path.substring("/users/".length())))
                    .filter(u -> u.getStatus() == UserStatus.ACTIVE);
            if (user.isPresent()) {
                title = user.get().getUsername() + " のプロフィール — Band Link";
                description = user.get().getBio() != null && !user.get().getBio().isBlank()
                        ? summarize(user.get().getBio())
                        : user.get().getUsername() + " さんのプロフィールページです。Band Linkでバンドメンバーの募集・加入希望をチェックできます。";
            }
        }
        model.addAttribute("seoTitle", title);
        model.addAttribute("seoDescription", description);
        return "posts";
    }

    private static String summarize(String text) {
        if (text == null || text.isBlank()) return DEFAULT_DESCRIPTION;
        String trimmed = text.strip().replaceAll("\\s+", " ");
        return trimmed.length() > 120 ? trimmed.substring(0, 120) + "…" : trimmed;
    }

    @GetMapping("/api/csrf")
    @ResponseBody
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }
}
