package com.example.bandlink.dto;

import com.example.bandlink.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public final class PostRequests {
    private PostRequests() {}
    // requirements.md §投稿: "パート・ジャンル・活動スタンスはそれぞれ1つ以上選ぶ" — one or more, no ceiling —
    // and 活動エリア alone is documented and UI-limited to 3 ("全国の都道府県から3つまで選択"). A max=5/3/1
    // cap on parts/genres/stances was added without a requirement or a matching UI limit; remove it rather
    // than let it reject posts the product is supposed to accept. Keep prefectureIds required: requirements.md
    // lists 活動エリア among the post's required fields.
    public record Create(
            @NotNull PostType type,
            @NotBlank @Size(max = 30) String title,
            @NotBlank @Size(max = 500) String content,
            @Size(max = 100) String areaSub,
            @NotEmpty Set<Long> partIds,
            @NotEmpty Set<Long> genreIds,
            @NotEmpty Set<Long> stanceIds,
            @NotEmpty @Size(max = 3) Set<Long> prefectureIds,
            @NotEmpty Set<AgeRange> ageRanges,
            @NotNull ActivityFrequency activityFrequency) {}
    public record Update(
            @NotBlank @Size(max = 30) String title,
            @NotBlank @Size(max = 500) String content,
            @Size(max = 100) String areaSub,
            @NotEmpty Set<Long> partIds,
            @NotEmpty Set<Long> genreIds,
            @NotEmpty Set<Long> stanceIds,
            @NotEmpty @Size(max = 3) Set<Long> prefectureIds,
            @NotEmpty Set<AgeRange> ageRanges,
            @NotNull ActivityFrequency activityFrequency) {}
}
