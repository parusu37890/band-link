package com.example.bandlink.service;

import com.example.bandlink.dto.PostRequests;
import com.example.bandlink.entity.*;
import com.example.bandlink.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {
    @Mock PostRepository posts; @Mock UserRepository users; @Mock PartRepository parts; @Mock GenreRepository genres;
    @Mock StanceRepository stances; @Mock PrefectureRepository prefectures; @Mock BlockRepository blocks;

    @Test
    void createRequiresVerifiedEmailAndAppliesEditLock() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-05T03:00:00Z"), ZoneId.of("Asia/Tokyo"));
        PostService service = new PostService(posts, users, parts, genres, stances, prefectures, blocks, clock);
        User user = new User("u", "u@example.com", "hash");
        when(users.findById(1L)).thenReturn(Optional.of(user));
        PostRequests.Create request = request();

        assertThrows(PostService.RuleViolationException.class, () -> service.create(1L, request));
        user.setEmailVerifiedAt(LocalDateTime.now(clock).minusHours(13));
        when(posts.existsByUserIdAndStatus(1L, PostStatus.OPEN)).thenReturn(false);
        when(posts.save(any(Post.class))).thenAnswer(i -> i.getArgument(0));
        Post created = service.create(1L, request);

        assertEquals(PostStatus.OPEN, created.getStatus());
        assertEquals(LocalDateTime.now(clock).plusDays(30), created.getExpiresAt());
        assertThrows(PostService.RuleViolationException.class, () -> service.create(1L, request));
    }

    @Test
    void listClosesExpiredOpenPostsLazily() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-05T03:00:00Z"), ZoneId.of("Asia/Tokyo"));
        PostService service = new PostService(posts, users, parts, genres, stances, prefectures, blocks, clock);
        LocalDateTime created = LocalDateTime.now(clock).minusDays(31);
        Post expired = new Post(new User("u", "u@example.com", "hash"), PostType.MEMBER_WANTED, "T", "C", null, ActivityFrequency.WEEKLY_1, created);
        when(posts.findByStatusOrderByRankUpdatedAtDesc(PostStatus.OPEN)).thenReturn(List.of(expired));

        assertTrue(service.listOpen().isEmpty());
        assertEquals(ClosedReason.EXPIRED, expired.getClosedReason());
    }

    private PostRequests.Create request() {
        return new PostRequests.Create(PostType.MEMBER_WANTED, "Title", "Content", "Shibuya",
                Set.of(), Set.of(), Set.of(), Set.of(), Set.of(AgeRange.ANY), ActivityFrequency.WEEKLY_1);
    }

    @Test void publicDetailKeepsExpiredContentButHidesModerationAndInactiveAuthors() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-05T03:00:00Z"), ZoneOffset.UTC);
        PostService service = new PostService(posts, users, parts, genres, stances, prefectures, blocks, clock);
        User author = new User("Author", "private@example.com", "hash");
        Post post = new Post(author, PostType.MEMBER_WANTED, "Title", "Content", null,
                ActivityFrequency.WEEKLY_1, LocalDateTime.now(clock).minusDays(31));
        when(posts.findById(1L)).thenReturn(Optional.of(post));
        assertSame(post, service.getPublic(1L));
        assertEquals(ClosedReason.EXPIRED, post.getClosedReason());
        post.close(ClosedReason.DELETED_BY_ADMIN, LocalDateTime.now(clock));
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.getPublic(1L));
        post.close(ClosedReason.MANUAL, LocalDateTime.now(clock));
        author.setStatus(UserStatus.WITHDRAWN);
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.getPublic(1L));
        author.setStatus(UserStatus.SUSPENDED);
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.getPublic(1L));
    }

    @Test
    void listingHidesBothSidesOfABlockWhileAnonymousBrowsingKeepsEverything() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-05T03:00:00Z"), ZoneId.of("Asia/Tokyo"));
        PostService service = new PostService(posts, users, parts, genres, stances, prefectures, blocks, clock);
        User viewer = identified(1L, "Viewer"), blockedByViewer = identified(2L, "Blocked"),
             blocksTheViewer = identified(3L, "Blocker"), unrelated = identified(4L, "Other");
        Post fromBlocked = openPost(blockedByViewer, clock), fromBlocker = openPost(blocksTheViewer, clock),
             fromUnrelated = openPost(unrelated, clock);
        when(posts.findAll(ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Post>>any(),
                any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(fromBlocked, fromBlocker, fromUnrelated));
        // One block made in each direction: requirements 8章 hides the counterpart either way.
        when(blocks.findByBlockerIdOrBlockedId(1L, 1L)).thenReturn(List.of(
                new Block(viewer, blockedByViewer, LocalDateTime.now(clock)),
                new Block(blocksTheViewer, viewer, LocalDateTime.now(clock))));

        assertEquals(List.of(fromUnrelated), service.searchFor(1L, null));
        // Anonymous browsing still sees them: blocking hides people from browsing, it is not access
        // control, which is what docs/decisions/0004 settled.
        assertEquals(3, service.search(null).size());
    }

    private User identified(Long id, String name) {
        User user = new User(name, name + "@example.com", "hash");
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return user;
    }

    private Post openPost(User author, Clock clock) {
        return new Post(author, PostType.MEMBER_WANTED, "T", "C", null, ActivityFrequency.WEEKLY_1,
                LocalDateTime.now(clock));
    }
}
