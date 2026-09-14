package com.example.bandlink.dto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
public record ProfileUpdateRequest(@NotBlank @Size(max=80) String username, @NotNull @Pattern(regexp="男性|女性", message="性別は男性または女性から選択してください。") String gender,
        @Size(max=1000) String bio, @NotNull @Min(0) @Max(120) Integer age,
        @NotNull @Min(0) @Max(100) Integer experienceYears, @Size(max=1000) String videoUrl,
        @Size(max=1000) String youtubeUrl, @Size(max=1000) String tiktokUrl,
        @Size(max=1000) String soundcloudUrl, @Size(max=1000) String spotifyUrl,
        @Size(max=1000) String appleMusicUrl,
        // requirements.md: "パート・ジャンル・活動スタンスはそれぞれ1つ以上選ぶ" (one or more, no ceiling) and
        // "都道府県・パート・ジャンル・活動スタンス...は複数選択可能" — only 活動エリア has a documented cap (3).
        // A max=5/3/1 cap was added here without a requirement behind it and with no matching UI limit
        // (the settings form renders all three as plain checkboxes, unlike the counted 活動エリア picker);
        // it rejected every existing profile that already had more than one stance or more than three
        // genres, including seeded demo accounts, on any save at all. Do not reintroduce it without a
        // requirement and a UI that enforces and explains it.
        @NotEmpty Set<Long> partIds, @NotEmpty Set<Long> genreIds, @NotEmpty Set<Long> stanceIds,
        @NotEmpty @Size(max=3) Set<Long> prefectureIds) {}
