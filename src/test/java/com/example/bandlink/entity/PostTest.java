package com.example.bandlink.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class PostTest {
    @Test
    void newPostStartsOpenForThirtyDays() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 5, 12, 0);
        Post post = new Post(new User("u", "u@example.com", "hash"), PostType.MEMBER_WANTED,
                "Title", "Content", "Shibuya", ActivityFrequency.WEEKLY_1, now);

        assertEquals(PostStatus.OPEN, post.getStatus());
        assertEquals(now.plusDays(30), post.getExpiresAt());
        assertEquals(now, post.getRankUpdatedAt());
    }

    @Test
    void reopenResetsExpiryWithoutChangingEditTimestamp() {
        LocalDateTime created = LocalDateTime.of(2026, 9, 5, 12, 0);
        LocalDateTime reopened = created.plusDays(31);
        Post post = new Post(new User("u", "u@example.com", "hash"), PostType.WANTS_TO_JOIN,
                "Title", "Content", null, ActivityFrequency.MONTHLY_1, created);
        post.close(ClosedReason.EXPIRED, created.plusDays(30));
        post.reopen(reopened);

        assertEquals(PostStatus.OPEN, post.getStatus());
        assertNull(post.getClosedReason());
        assertEquals(reopened.plusDays(30), post.getExpiresAt());
        assertEquals(created, post.getUpdatedAt());
    }
}
