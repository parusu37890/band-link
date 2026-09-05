package com.example.bandlink.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "search_histories", indexes = @Index(name = "ix_search_history_user_time", columnList = "user_id,searched_at"))
public class SearchHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false, columnDefinition = "text") private String conditions;
    @Column(nullable = false, length = 64) private String conditionsHash;
    @Column(nullable = false) private LocalDateTime searchedAt;
    protected SearchHistory() {}
    public SearchHistory(User user, String conditions, String conditionsHash, LocalDateTime searchedAt) { this.user = user; this.conditions = conditions; this.conditionsHash = conditionsHash; this.searchedAt = searchedAt; }
    public Long getId() { return id; }
    public String getConditions() { return conditions; }
    public String getConditionsHash() { return conditionsHash; }
    public LocalDateTime getSearchedAt() { return searchedAt; }
    public void touch(LocalDateTime now) { this.searchedAt = now; }
}
