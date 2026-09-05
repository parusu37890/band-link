package com.example.bandlink.repository;

import com.example.bandlink.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    Optional<SearchHistory> findByUserIdAndConditionsHash(Long userId, String conditionsHash);
    List<SearchHistory> findTop5ByUserIdOrderBySearchedAtDesc(Long userId);
    void deleteByUserIdAndIdNotIn(Long userId, List<Long> ids);
}
