package com.example.bandlink.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Maps a LINE user id to the local account created for Band Link. */
@Entity
@Table(name = "line_accounts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_line_accounts_user_id", columnNames = "user_id"),
        @UniqueConstraint(name = "uk_line_accounts_line_user_id", columnNames = "line_user_id")
})
public class LineAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "line_user_id", nullable = false, length = 64)
    private String lineUserId;
    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected LineAccount() {}

    public LineAccount(User user, String lineUserId) {
        this.user = user;
        this.lineUserId = lineUserId;
    }

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getLineUserId() { return lineUserId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
