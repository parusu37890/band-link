package com.example.bandlink.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "posts", indexes = @Index(name = "ix_posts_status_rank", columnList = "status,rank_updated_at"))
public class Post {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PostType type;
    @Column(nullable = false, length = 100) private String title;
    @Column(nullable = false, columnDefinition = "text") private String content;
    @Column(length = 100) private String areaSub;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ActivityFrequency activityFrequency;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PostStatus status = PostStatus.OPEN;
    @Enumerated(EnumType.STRING) @Column(length = 30) private ClosedReason closedReason;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    @Column private LocalDateTime closedAt;
    @Column(nullable = false) private LocalDateTime expiresAt;
    @Column(nullable = false) private LocalDateTime rankUpdatedAt;

    @ManyToMany @JoinTable(name = "post_parts", joinColumns = @JoinColumn(name = "post_id"), inverseJoinColumns = @JoinColumn(name = "part_id"))
    private Set<Part> parts = new HashSet<>();
    @ManyToMany @JoinTable(name = "post_genres", joinColumns = @JoinColumn(name = "post_id"), inverseJoinColumns = @JoinColumn(name = "genre_id"))
    private Set<Genre> genres = new HashSet<>();
    @ManyToMany @JoinTable(name = "post_stances", joinColumns = @JoinColumn(name = "post_id"), inverseJoinColumns = @JoinColumn(name = "stance_id"))
    private Set<Stance> stances = new HashSet<>();
    @ManyToMany @JoinTable(name = "post_prefectures", joinColumns = @JoinColumn(name = "post_id"), inverseJoinColumns = @JoinColumn(name = "prefecture_id"))
    private Set<Prefecture> prefectures = new HashSet<>();
    @ElementCollection @Enumerated(EnumType.STRING) @CollectionTable(name = "post_age_ranges", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "age_range", nullable = false, length = 20) private Set<AgeRange> ageRanges = new HashSet<>();

    protected Post() {}
    public Post(User user, PostType type, String title, String content, String areaSub, ActivityFrequency activityFrequency,
                LocalDateTime now) {
        this.user = user; this.type = type; this.title = title; this.content = content; this.areaSub = areaSub;
        this.activityFrequency = activityFrequency; this.createdAt = now; this.updatedAt = now;
        this.expiresAt = now.plusDays(30); this.rankUpdatedAt = now;
    }
    public Long getId() { return id; }
    public User getUser() { return user; }
    public PostType getType() { return type; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getAreaSub() { return areaSub; }
    public ActivityFrequency getActivityFrequency() { return activityFrequency; }
    public PostStatus getStatus() { return status; }
    public ClosedReason getClosedReason() { return closedReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getRankUpdatedAt() { return rankUpdatedAt; }
    public Set<Part> getParts() { return parts; }
    public Set<Genre> getGenres() { return genres; }
    public Set<Stance> getStances() { return stances; }
    public Set<Prefecture> getPrefectures() { return prefectures; }
    public Set<AgeRange> getAgeRanges() { return ageRanges; }
    public void update(String title, String content, String areaSub, ActivityFrequency frequency, LocalDateTime now) {
        this.title = title; this.content = content; this.areaSub = areaSub; this.activityFrequency = frequency; this.updatedAt = now;
    }
    public void close(ClosedReason reason, LocalDateTime now) { this.status = PostStatus.CLOSED; this.closedReason = reason; this.closedAt = now; }
    public void reopen(LocalDateTime now) { this.status = PostStatus.OPEN; this.closedReason = null; this.closedAt = null; this.expiresAt = now.plusDays(30); }
    public void boostRank(LocalDateTime now) { this.rankUpdatedAt = now; }
}
