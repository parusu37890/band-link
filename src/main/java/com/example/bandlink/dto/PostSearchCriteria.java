package com.example.bandlink.dto;

import com.example.bandlink.entity.ActivityFrequency;
import com.example.bandlink.entity.AgeRange;
import java.util.Set;

public record PostSearchCriteria(String keyword, Set<Long> prefectureIds, Set<Long> partIds, Set<Long> genreIds,
                                 Set<Long> stanceIds, Set<AgeRange> ageRanges, Set<ActivityFrequency> activityFrequencies) {
    /**
     * The keyword slot remains in the wire record for backwards-compatible JSON/query binding,
     * but free-text search was removed from the product.  Only the explicit selection filters
     * constitute a search condition now.
     */
    public boolean hasConditions() {
        return java.util.Arrays.stream(new Set<?>[]{prefectureIds, partIds, genreIds, stanceIds,
                ageRanges, activityFrequencies}).anyMatch(s -> s != null && !s.isEmpty());
    }
}
