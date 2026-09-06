package com.example.bandlink.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens", indexes = @Index(name = "ix_password_reset_token", columnList = "token", unique = true))
public class PasswordResetToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false, unique = true, length = 100) private String token;
    @Column(nullable = false) private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    @Column(nullable = false) private LocalDateTime createdAt;
    protected PasswordResetToken() {}
    public PasswordResetToken(User user, String token, LocalDateTime expiresAt) { this.user = user; this.token = token; this.expiresAt = expiresAt; }
    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getToken() { return token; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
    public boolean isUsableAt(LocalDateTime now) { return usedAt == null && expiresAt.isAfter(now); }
}
