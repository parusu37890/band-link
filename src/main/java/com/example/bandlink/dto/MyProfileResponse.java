package com.example.bandlink.dto;

import com.example.bandlink.entity.User;
import java.util.List;

public record MyProfileResponse(Long id, String username, String bio, String gender, String ageRange,
        Integer age, Integer experienceYears, String videoUrl, String youtubeUrl, String tiktokUrl,
        String soundcloudUrl, String spotifyUrl, String appleMusicUrl, String profileImageUrl,
        List<MasterOption> parts, List<MasterOption> genres, List<MasterOption> stances,
        List<MasterOption> prefectures, String email, boolean emailVerified, String status, String role) {
    public static MyProfileResponse from(User u) {
        ProfileResponse p = ProfileResponse.from(u);
        return new MyProfileResponse(p.id(), p.username(), p.bio(), p.gender(), p.ageRange(), u.getAge(),
                p.experienceYears(), p.videoUrl(), p.youtubeUrl(), p.tiktokUrl(), p.soundcloudUrl(), p.spotifyUrl(),
                p.appleMusicUrl(), p.profileImageUrl(), p.parts(), p.genres(), p.stances(),
                p.prefectures(), u.getEmail(), u.isEmailVerified(), u.getStatus().name(), u.getRole().name());
    }
}
