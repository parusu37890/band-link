package com.example.bandlink.dto;
import com.example.bandlink.entity.SearchHistory;
import java.time.LocalDateTime;
public record SearchHistoryResponse(String conditions, LocalDateTime searchedAt) {
    public static SearchHistoryResponse from(SearchHistory h) { return new SearchHistoryResponse(h.getConditions(), h.getSearchedAt()); }
}
