package com.example.bandlink.dto;

import com.example.bandlink.entity.Post;

public record PostResponse(Long id, Long userId, String username, String title, String content,
                           String status, String closedReason, java.time.LocalDateTime expiresAt,
                           java.time.LocalDateTime rankUpdatedAt, String type, String areaSub,
                           String activityFrequency, java.util.Set<com.example.bandlink.entity.AgeRange> ageRanges,
                           java.util.List<MasterOption> parts, java.util.List<MasterOption> genres,
                           java.util.List<MasterOption> stances, java.util.List<MasterOption> prefectures,
                           java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {
    public static PostResponse from(Post post) {
        return new PostResponse(post.getId(), post.getUser().getId(), post.getUser().getUsername(), post.getTitle(),
                post.getContent(), post.getStatus().name(), post.getClosedReason() == null ? null : post.getClosedReason().name(),
                post.getExpiresAt(), post.getRankUpdatedAt(), post.getType().name(), post.getAreaSub(),
                post.getActivityFrequency().name(), java.util.Set.copyOf(post.getAgeRanges()),
                post.getParts().stream().map(p -> new MasterOption(p.getId(), p.getName())).toList(),
                post.getGenres().stream().map(p -> new MasterOption(p.getId(), p.getName())).toList(),
                post.getStances().stream().map(p -> new MasterOption(p.getId(), p.getName())).toList(),
                post.getPrefectures().stream().map(p -> new MasterOption(p.getId(), p.getName())).toList(),
                post.getCreatedAt(), post.getUpdatedAt());
    }
}
