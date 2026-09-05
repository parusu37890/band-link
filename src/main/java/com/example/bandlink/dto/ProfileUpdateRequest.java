package com.example.bandlink.dto;
import jakarta.validation.constraints.Size;
import java.util.Set;
public record ProfileUpdateRequest(@Size(max=80) String username, @Size(max=40) String gender,
        @Size(max=1000) String bio, Integer age, Integer experienceYears, @Size(max=1000) String videoUrl,
        Set<Long> partIds, Set<Long> genreIds, Set<Long> stanceIds, Set<Long> prefectureIds) {}
