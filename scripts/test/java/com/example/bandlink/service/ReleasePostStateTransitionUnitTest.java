package com.example.bandlink.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.bandlink.entity.ActivityFrequency;
import com.example.bandlink.entity.ClosedReason;
import com.example.bandlink.entity.Post;
import com.example.bandlink.entity.PostStatus;
import com.example.bandlink.entity.PostType;
import com.example.bandlink.entity.User;
import com.example.bandlink.repository.BlockRepository;
import com.example.bandlink.repository.GenreRepository;
import com.example.bandlink.repository.PartRepository;
import com.example.bandlink.repository.PostRepository;
import com.example.bandlink.repository.PrefectureRepository;
import com.example.bandlink.repository.StanceRepository;
import com.example.bandlink.repository.UserRepository;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Real PostService state-machine assertions for the release matrix. */
class ReleasePostStateTransitionUnitTest {
    private final PostRepository posts = mock(PostRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final PartRepository parts = mock(PartRepository.class);
    private final GenreRepository genres = mock(GenreRepository.class);
    private final StanceRepository stances = mock(StanceRepository.class);
    private final PrefectureRepository prefectures = mock(PrefectureRepository.class);
    private final BlockRepository blocks = mock(BlockRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-14T00:00:00Z"), ZoneOffset.UTC);
    private PostService service;
    private User owner;

    @BeforeEach
    void setUp() {
        service = new PostService(posts, users, parts, genres, stances, prefectures, blocks, clock);
        owner = new User("owner", "owner@qa.invalid", "hash");
        setId(owner, 101L);
        owner.setEmailVerifiedAt(LocalDateTime.now(clock).minusDays(1));
        when(users.findById(101L)).thenReturn(Optional.of(owner));
        when(parts.findAllById(any())).thenReturn(List.of());
        when(genres.findAllById(any())).thenReturn(List.of());
        when(stances.findAllById(any())).thenReturn(List.of());
        when(prefectures.findAllById(any())).thenReturn(List.of());
    }

    @Test
    void manualCloseThenReopenReturnsToOpenAndRefreshesExpiry() {
        Post post = post(LocalDateTime.now(clock).minusDays(1));
        when(posts.findByIdAndUserId(7L, 101L)).thenReturn(Optional.of(post));
        when(posts.existsByUserIdAndStatus(101L, PostStatus.OPEN)).thenReturn(false);

        service.close(101L, 7L);
        assertEquals(PostStatus.CLOSED, post.getStatus());
        assertEquals(ClosedReason.MANUAL, post.getClosedReason());

        service.reopen(101L, 7L);
        assertEquals(PostStatus.OPEN, post.getStatus());
        assertEquals(null, post.getClosedReason());
        assertEquals(LocalDateTime.now(clock).plusDays(30), post.getExpiresAt());
    }

    @Test
    void ownerOnlyMutationRejectsAnotherUserEvenWhenPostExists() {
        Post post = post(LocalDateTime.now(clock));
        when(posts.findByIdAndUserId(7L, 101L)).thenReturn(Optional.empty());
        when(posts.existsById(7L)).thenReturn(true);
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.close(101L, 7L));
        assertEquals(PostStatus.OPEN, post.getStatus());
    }

    @Test
    void adminDeletedPostCannotBeReopened() {
        Post post = post(LocalDateTime.now(clock));
        post.close(ClosedReason.DELETED_BY_ADMIN, LocalDateTime.now(clock));
        when(posts.findByIdAndUserId(7L, 101L)).thenReturn(Optional.of(post));
        assertThrows(PostService.RuleViolationException.class, () -> service.reopen(101L, 7L));
        assertEquals(PostStatus.CLOSED, post.getStatus());
    }

    private Post post(LocalDateTime createdAt) {
        return new Post(owner, PostType.MEMBER_WANTED, "title", "content", "area",
                ActivityFrequency.WEEKLY_1, createdAt);
    }

    private void setId(Object target, long id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}
