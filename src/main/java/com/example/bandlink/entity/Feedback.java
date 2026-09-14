package com.example.bandlink.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feedback")
public class Feedback {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) FeedbackType type;
    @Column(nullable = false, columnDefinition = "text") String messageText;
    @Column(length = 1000) String imageUrl;
    @Column(nullable = false) LocalDateTime createdAt;

    protected Feedback() {}

    public Feedback(User user, FeedbackType type, String messageText, String imageUrl, LocalDateTime createdAt) {
        this.user = user;
        this.type = type;
        this.messageText = messageText;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public FeedbackType getType() { return type; }
    public String getMessageText() { return messageText; }
    public String getImageUrl() { return imageUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
