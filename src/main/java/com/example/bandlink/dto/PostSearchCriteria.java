package com.example.bandlink.dto;

import com.example.bandlink.entity.ActivityFrequency;
import java.util.Set;

public record PostSearchCriteria(String keyword, Set<ActivityFrequency> activityFrequencies) {
    public boolean hasConditions() { return (keyword != null && !keyword.isBlank()) || (activityFrequencies != null && !activityFrequencies.isEmpty()); }
}
