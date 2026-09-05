package com.example.bandlink.dto;

import com.example.bandlink.entity.Post;

public record PostResponse(Long id, Long userId, String username, String title, String content,
                           String status, String closedReason, java.time.LocalDateTime expiresAt,
                           java.time.LocalDateTime rankUpdatedAt) {
    public static PostResponse from(Post post) {
        return new PostResponse(post.getId(), post.getUser().getId(), post.getUser().getUsername(), post.getTitle(),
                post.getContent(), post.getStatus().name(), post.getClosedReason() == null ? null : post.getClosedReason().name(),
                post.getExpiresAt(), post.getRankUpdatedAt());
    }
}
