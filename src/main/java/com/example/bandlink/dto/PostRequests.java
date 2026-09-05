package com.example.bandlink.dto;

import com.example.bandlink.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public final class PostRequests {
    private PostRequests() {}
    public record Create(
            @NotNull PostType type,
            @NotBlank @Size(max = 100) String title,
            @NotBlank @Size(max = 2000) String content,
            @Size(max = 100) String areaSub,
            @NotEmpty Set<Long> partIds,
            @NotEmpty Set<Long> genreIds,
            @NotEmpty Set<Long> stanceIds,
            @Size(max = 3) Set<Long> prefectureIds,
            @NotEmpty Set<AgeRange> ageRanges,
            @NotNull ActivityFrequency activityFrequency) {}
    public record Update(
            @NotBlank @Size(max = 100) String title,
            @NotBlank @Size(max = 2000) String content,
            @Size(max = 100) String areaSub,
            @NotEmpty Set<Long> partIds,
            @NotEmpty Set<Long> genreIds,
            @NotEmpty Set<Long> stanceIds,
            @Size(max = 3) Set<Long> prefectureIds,
            @NotEmpty Set<AgeRange> ageRanges,
            @NotNull ActivityFrequency activityFrequency) {}
}
