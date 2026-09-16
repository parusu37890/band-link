package com.example.bandlink.dto;

import com.example.bandlink.entity.Conversation;
import java.time.LocalDateTime;

public record ConversationResponse(Long id, PeerResponse otherUser, LocalDateTime lastMessageAt, long unreadCount) {
    public static ConversationResponse from(Conversation c, Long viewerId, long unreadCount) {
        if (!c.includes(viewerId)) throw new org.springframework.security.access.AccessDeniedException("会話を閲覧できません");
        return new ConversationResponse(c.getId(), PeerResponse.from(c.getUserA().getId().equals(viewerId)
                ? c.getUserB() : c.getUserA()), c.getLastMessageAt(), unreadCount);
    }
}
