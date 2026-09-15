package com.example.bandlink.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.bandlink.repository.PostRepository;
import com.example.bandlink.repository.UserRepository;
import org.junit.jupiter.api.Test;

class SeoControllerTest {
    private final PostRepository posts = mock(PostRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final SeoController controller = new SeoController(posts, users, "https://band-link.example/");

    @Test
    void robotsExposesSitemapAndKeepsPrivateScreensOut() {
        String body = controller.robots().getBody();

        assertThat(body).contains("Allow: /posts", "Allow: /users/", "Disallow: /api/",
                "Disallow: /messages", "Sitemap: https://band-link.example/sitemap.xml");
    }

    @Test
    void sitemapContainsOnlyPublicRootsWhenThereIsNoPublicData() {
        when(posts.findByStatusOrderByRankUpdatedAtDesc(com.example.bandlink.entity.PostStatus.OPEN))
                .thenReturn(java.util.List.of());
        when(users.findByStatusOrderByIdAsc(com.example.bandlink.entity.UserStatus.ACTIVE))
                .thenReturn(java.util.List.of());

        String body = controller.sitemap().getBody();

        assertThat(body).contains("<urlset", "<loc>https://band-link.example/posts</loc>")
                .endsWith("</urlset>");
    }
}
