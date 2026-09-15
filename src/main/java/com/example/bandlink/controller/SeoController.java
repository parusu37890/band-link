package com.example.bandlink.controller;

import com.example.bandlink.entity.PostStatus;
import com.example.bandlink.entity.UserStatus;
import com.example.bandlink.repository.PostRepository;
import com.example.bandlink.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public crawler entry points. Private application screens are deliberately excluded. */
@RestController
public class SeoController {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private final PostRepository posts;
    private final UserRepository users;
    private final String baseUrl;

    public SeoController(PostRepository posts, UserRepository users,
                         @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.posts = posts;
        this.users = users;
        this.baseUrl = trimTrailingSlash(baseUrl);
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> robots() {
        String body = "User-agent: *\n"
                + "Allow: /\n"
                + "Allow: /posts\n"
                + "Allow: /users/\n"
                // Public pages are client-rendered: the browser fetches these three read-only
                // endpoints to fill in the content after the initial (empty) HTML load. Blocking
                // all of /api/ blocked Googlebot's own renderer from fetching them too, so it only
                // ever saw the loading placeholder and flagged every page as a soft 404.
                + "Allow: /api/posts/\n"
                + "Allow: /api/users/\n"
                + "Allow: /api/masters\n"
                + "Disallow: /api/\n"
                + "Disallow: /admin\n"
                + "Disallow: /settings\n"
                + "Disallow: /messages\n"
                + "Disallow: /notifications\n"
                + "Disallow: /blocks\n"
                + "Disallow: /my/\n"
                + "Disallow: /posts/new\n"
                + "Disallow: /posts/*/edit\n"
                + "Disallow: /login\n"
                + "Disallow: /register\n"
                + "Disallow: /password-reset\n"
                + "Disallow: /verify-email\n"
                + "Disallow: /contact\n"
                + "Disallow: /feature-request\n"
                + "Sitemap: " + baseUrl + "/sitemap.xml\n";
        return ResponseEntity.ok(body);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
        appendUrl(xml, baseUrl + "/posts", LocalDate.now());
        posts.findByStatusOrderByRankUpdatedAtDesc(PostStatus.OPEN).forEach(post ->
                appendUrl(xml, baseUrl + "/posts/" + post.getId(), date(post.getRankUpdatedAt())));
        users.findByStatusOrderByIdAsc(UserStatus.ACTIVE).forEach(user ->
                appendUrl(xml, baseUrl + "/users/" + user.getId(), date(user.getCreatedAt())));
        return ResponseEntity.ok(xml.append("</urlset>").toString());
    }

    private void appendUrl(StringBuilder xml, String location, LocalDate lastModified) {
        xml.append("<url><loc>").append(escape(location)).append("</loc>");
        if (lastModified != null) xml.append("<lastmod>").append(lastModified.format(DATE)).append("</lastmod>");
        xml.append("</url>");
    }

    private static LocalDate date(LocalDateTime value) {
        return value == null ? null : value.toLocalDate();
    }

    private static String trimTrailingSlash(String value) {
        String result = value == null || value.isBlank() ? "http://localhost:8080" : value.trim();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
}
