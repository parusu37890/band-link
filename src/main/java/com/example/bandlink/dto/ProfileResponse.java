package com.example.bandlink.dto;

import com.example.bandlink.entity.User;
import java.util.List;

public record ProfileResponse(Long id, String username, String bio, String gender, String ageRange,
        Integer experienceYears, String videoUrl, String profileImageUrl, String activity, List<MasterOption> parts,
        List<MasterOption> genres, List<MasterOption> stances, List<MasterOption> prefectures) {
    public static ProfileResponse from(User u) {
        return new ProfileResponse(u.getId(), u.getUsername(), u.getBio(), u.getGender(),
                u.getAge() == null ? null : (u.getAge() / 10 * 10) + "代", u.getExperienceYears(),
                u.getVideoUrl(), u.getProfileImageUrl(), ActivitySignal.of(u.getLastLoginAt()),
                u.getParts().stream().map(p -> new MasterOption(p.getId(), p.getName())).toList(),
                u.getGenres().stream().map(p -> new MasterOption(p.getId(), p.getName())).toList(),
                u.getStances().stream().map(p -> new MasterOption(p.getId(), p.getName())).toList(),
                u.getPrefectures().stream().map(p -> new MasterOption(p.getId(), p.getName())).toList());
    }
}
