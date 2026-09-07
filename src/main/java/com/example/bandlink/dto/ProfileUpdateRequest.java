package com.example.bandlink.dto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
public record ProfileUpdateRequest(@Size(max=80) String username, @NotNull @Pattern(regexp="男|女", message="性別は男または女から選択してください。") String gender,
        @Size(max=1000) String bio, @NotNull @Min(0) @Max(120) Integer age,
        @NotNull @Min(0) @Max(100) Integer experienceYears, @Size(max=1000) String videoUrl,
        @Size(max=1000) String youtubeUrl, @Size(max=1000) String tiktokUrl,
        @Size(max=1000) String soundcloudUrl, @Size(max=1000) String spotifyUrl,
        @Size(max=1000) String appleMusicUrl,
        @NotEmpty Set<Long> partIds, @NotEmpty Set<Long> genreIds, @NotEmpty Set<Long> stanceIds,
        @NotEmpty Set<Long> prefectureIds) {}
