package com.example.bandlink.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Maps an X (Twitter) user id to the local account created for Band Link. */
@Entity
@Table(name = "x_accounts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_x_accounts_user_id", columnNames = "user_id"),
        @UniqueConstraint(name = "uk_x_accounts_x_user_id", columnNames = "x_user_id")
})
public class XAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "x_user_id", nullable = false, length = 64)
    private String xUserId;
    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected XAccount() {}

    public XAccount(User user, String xUserId) {
        this.user = user;
        this.xUserId = xUserId;
    }

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getXUserId() { return xUserId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
