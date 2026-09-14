package com.example.bandlink.dto;

import com.example.bandlink.entity.Feedback;
import java.time.LocalDateTime;

public record FeedbackResponse(Long id, String type, String username, String message,
                               String imageUrl, LocalDateTime createdAt) {
    public static FeedbackResponse from(Feedback feedback) {
        return new FeedbackResponse(feedback.getId(), feedback.getType().name(),
                feedback.getUser().getUsername(), feedback.getMessageText(),
                feedback.getImageUrl(), feedback.getCreatedAt());
    }
}
