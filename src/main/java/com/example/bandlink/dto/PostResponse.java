package com.example.bandlink.dto;

import com.example.bandlink.entity.Post;
import com.example.bandlink.entity.UserStatus;

public record PostResponse(Long id, Long userId, String username, String authorActivity, String authorImageUrl,
                           String authorAgeRange, boolean authorOnline,
                           String title, String content,
                           String status, String closedReason, java.time.LocalDateTime expiresAt,
                           java.time.LocalDateTime rankUpdatedAt, String type, String areaSub,
                           String activityFrequency, java.util.Set<com.example.bandlink.entity.AgeRange> ageRanges,
                           java.util.List<MasterOption> parts, java.util.List<MasterOption> genres,
                           java.util.List<MasterOption> stances, java.util.List<MasterOption> prefectures,
                           java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {
    public static PostResponse from(Post post) {
        return new PostResponse(post.getId(), post.getUser().getId(), post.getUser().getUsername(),
                ActivitySignal.of(post.getUser().getLastLoginAt()), authorImage(post),
                AgeBand.of(post.getUser().getAge()), online(post),
                post.getTitle(),
                post.getContent(), post.getStatus().name(), post.getClosedReason() == null ? null : post.getClosedReason().name(),
                post.getExpiresAt(), post.getRankUpdatedAt(), post.getType().name(), post.getAreaSub(),
                post.getActivityFrequency().name(), java.util.Set.copyOf(post.getAgeRanges()),
                post.getParts().stream().map(p -> new MasterOption(p.getId(), p.getName())).toList(),
                post.getGenres().stream().map(p -> new MasterOption(p.getId(), p.getName())).toList(),
                post.getStances().stream().map(p -> new MasterOption(p.getId(), p.getName())).toList(),
                post.getPrefectures().stream().map(p -> new MasterOption(p.getId(), p.getName())).toList(),
                post.getCreatedAt(), post.getUpdatedAt());
    }

    /**
     * The listing showed the poster as initials because it was never given their picture. It is
     * given one now, under the same rule the rest of the app follows (docs/decisions/0001): a
     * withdrawn or suspended account's photo stays hidden, and comes back if a suspension lifts.
     */
    private static String authorImage(Post post) {
        return post.getUser().getStatus() == UserStatus.ACTIVE ? post.getUser().getProfileImageUrl() : null;
    }

    /**
     * Only an active account is shown as online. A suspended one may still hold a session, and
     * presence is the one signal that would keep saying so while the rest of the row hides them.
     */
    private static boolean online(Post post) {
        return post.getUser().getStatus() == UserStatus.ACTIVE
                && ActivitySignal.isOnline(post.getUser().getLastSeenAt());
    }
}
