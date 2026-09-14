package com.example.bandlink.dto;

import com.example.bandlink.entity.User;
import com.example.bandlink.entity.UserStatus;
import java.util.List;

public record ProfileResponse(Long id, String username, String bio, String gender, String ageRange,
        Integer experienceYears, String videoUrl, String youtubeUrl, String tiktokUrl, String soundcloudUrl,
        String spotifyUrl, String appleMusicUrl, String profileImageUrl, String activity, boolean online,
        List<MasterOption> parts, List<MasterOption> genres, List<MasterOption> stances,
        List<MasterOption> prefectures) {
    public static ProfileResponse from(User u) {
        // Mirrors PostResponse's rule: presence is read from lastSeenAt (not lastLoginAt, which can be
        // days stale for someone reading right now), and only an ACTIVE account is ever shown as online.
        boolean online = u.getStatus() == UserStatus.ACTIVE && ActivitySignal.isOnline(u.getLastSeenAt());
        return new ProfileResponse(u.getId(), u.getUsername(), u.getBio(), u.getGender(),
                AgeBand.of(u.getAge()), u.getExperienceYears(),
                u.getVideoUrl(), u.getYoutubeUrl(), u.getTiktokUrl(), u.getSoundcloudUrl(), u.getSpotifyUrl(),
                u.getAppleMusicUrl(), u.getProfileImageUrl(), ActivitySignal.of(u.getLastLoginAt()), online,
                u.getParts().stream().map(p -> new MasterOption(p.getId(), p.getName())).toList(),
                u.getGenres().stream().map(p -> new MasterOption(p.getId(), p.getName())).toList(),
                u.getStances().stream().map(p -> new MasterOption(p.getId(), p.getName())).toList(),
                u.getPrefectures().stream().map(p -> new MasterOption(p.getId(), p.getName())).toList());
    }
}
