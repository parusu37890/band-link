package com.example.bandlink.service;

import com.example.bandlink.dto.PostSearchCriteria;
import com.example.bandlink.entity.SearchHistory;
import com.example.bandlink.entity.User;
import com.example.bandlink.repository.SearchHistoryRepository;
import com.example.bandlink.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SearchHistoryService {
    private final SearchHistoryRepository histories; private final UserRepository users; private final Clock clock;
    public SearchHistoryService(SearchHistoryRepository histories, UserRepository users) { this(histories, users, Clock.systemDefaultZone()); }
    SearchHistoryService(SearchHistoryRepository histories, UserRepository users, Clock clock) { this.histories = histories; this.users = users; this.clock = clock == null ? Clock.systemDefaultZone() : clock; }

    @Transactional
    public void record(Long userId, PostSearchCriteria criteria) {
        if (criteria == null || !criteria.hasConditions()) return;
        User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません"));
        String conditions = canonical(criteria); String hash = sha256(conditions); LocalDateTime now = LocalDateTime.now(clock);
        SearchHistory history = histories.findByUserIdAndConditionsHash(userId, hash).orElseGet(() -> new SearchHistory(user, conditions, hash, now));
        history.touch(now); histories.save(history);
        List<Long> keep = histories.findTop5ByUserIdOrderBySearchedAtDesc(userId).stream().map(SearchHistory::getId).filter(Objects::nonNull).toList();
        if (!keep.isEmpty()) histories.deleteByUserIdAndIdNotIn(userId, keep);
    }

    public List<SearchHistory> recent(Long userId) { return histories.findTop5ByUserIdOrderBySearchedAtDesc(userId); }
    private String canonical(PostSearchCriteria c) { return "keyword=" + val(c.keyword()) + "|prefectures=" + sorted(c.prefectureIds()) + "|parts=" + sorted(c.partIds()) + "|genres=" + sorted(c.genreIds()) + "|stances=" + sorted(c.stanceIds()) + "|ages=" + sorted(c.ageRanges()) + "|frequency=" + sorted(c.activityFrequencies()); }
    private String val(String s) { return s == null ? "" : s.trim().toLowerCase(Locale.ROOT); }
    private String sorted(Collection<?> values) { if (values == null) return ""; return values.stream().map(String::valueOf).sorted().reduce((a,b) -> a + "," + b).orElse(""); }
    private String sha256(String value) { try { byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); StringBuilder out = new StringBuilder(); for (byte b : digest) out.append(String.format("%02x", b)); return out.toString(); } catch (Exception e) { throw new IllegalStateException(e); } }
}
