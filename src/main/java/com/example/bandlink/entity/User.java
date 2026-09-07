package com.example.bandlink.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80)
    private String username;
    @Column(nullable = false, length = 320)
    private String email;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;
    private Integer age;
    @Column(length = 40) private String gender;
    @Column(columnDefinition = "text") private String bio;
    private Integer experienceYears;
    @Column(length = 1000) private String videoUrl;
    @Column(length = 1000) private String youtubeUrl;
    @Column(length = 1000) private String tiktokUrl;
    @Column(length = 1000) private String soundcloudUrl;
    @Column(length = 1000) private String spotifyUrl;
    @Column(length = 1000) private String appleMusicUrl;
    @Column(length = 1000) private String profileImageUrl;
    private LocalDateTime lastEditedAt;
    private LocalDateTime lastRankBoostedAt;
    // Recorded on login so a viewer can tell whether a poster is still around before writing to them.
    private LocalDateTime lastLoginAt;
    // Touched while a signed-in account is using the site. lastLoginAt only moves at sign-in, so it
    // says nothing about whether someone is here now; presence needs its own timestamp.
    private LocalDateTime lastSeenAt;
    @Column(nullable = false) private LocalDateTime createdAt;

    @ManyToMany
    @JoinTable(name = "user_parts", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "part_id"))
    private Set<Part> parts = new HashSet<>();
    @ManyToMany
    @JoinTable(name = "user_genres", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "genre_id"))
    private Set<Genre> genres = new HashSet<>();
    @ManyToMany
    @JoinTable(name = "user_stances", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "stance_id"))
    private Set<Stance> stances = new HashSet<>();
    @ManyToMany
    @JoinTable(name = "user_prefectures", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "prefecture_id"))
    private Set<Prefecture> prefectures = new HashSet<>();

    protected User() {}
    public User(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
    }
    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public LocalDateTime getEmailVerifiedAt() { return emailVerifiedAt; }
    public void setEmailVerifiedAt(LocalDateTime value) { this.emailVerifiedAt = value; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public String getYoutubeUrl() { return youtubeUrl; }
    public void setYoutubeUrl(String value) { this.youtubeUrl = value; }
    public String getTiktokUrl() { return tiktokUrl; }
    public void setTiktokUrl(String value) { this.tiktokUrl = value; }
    public String getSoundcloudUrl() { return soundcloudUrl; }
    public void setSoundcloudUrl(String value) { this.soundcloudUrl = value; }
    public String getSpotifyUrl() { return spotifyUrl; }
    public void setSpotifyUrl(String value) { this.spotifyUrl = value; }
    public String getAppleMusicUrl() { return appleMusicUrl; }
    public void setAppleMusicUrl(String value) { this.appleMusicUrl = value; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public LocalDateTime getLastEditedAt() { return lastEditedAt; }
    public void setLastEditedAt(LocalDateTime value) { this.lastEditedAt = value; }
    public LocalDateTime getLastRankBoostedAt() { return lastRankBoostedAt; }
    public void setLastRankBoostedAt(LocalDateTime value) { this.lastRankBoostedAt = value; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void touchLogin(LocalDateTime now) { this.lastLoginAt = now; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void touchSeen(LocalDateTime now) { this.lastSeenAt = now; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Set<Part> getParts() { return parts; }
    public Set<Genre> getGenres() { return genres; }
    public Set<Stance> getStances() { return stances; }
    public Set<Prefecture> getPrefectures() { return prefectures; }
    public boolean isEmailVerified() { return emailVerifiedAt != null; }
    public boolean isActive() { return status == UserStatus.ACTIVE; }
}
