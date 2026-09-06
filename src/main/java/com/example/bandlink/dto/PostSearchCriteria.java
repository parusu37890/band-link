package com.example.bandlink.dto;

import com.example.bandlink.entity.ActivityFrequency;
import com.example.bandlink.entity.AgeRange;
import java.util.Set;

public record PostSearchCriteria(String keyword, Set<Long> prefectureIds, Set<Long> partIds, Set<Long> genreIds,
                                 Set<Long> stanceIds, Set<AgeRange> ageRanges, Set<ActivityFrequency> activityFrequencies) {
    public boolean hasConditions() {
        return (keyword != null && !keyword.isBlank()) || java.util.Arrays.stream(new Set<?>[]{prefectureIds, partIds, genreIds, stanceIds, ageRanges, activityFrequencies}).anyMatch(s -> s != null && !s.isEmpty());
    }
}
