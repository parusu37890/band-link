package com.example.bandlink.dto;
import com.example.bandlink.entity.Message; import java.time.LocalDateTime;
public record MessageResponse(Long id,Long conversationId,Long senderId,String content,String imageUrl,LocalDateTime createdAt,LocalDateTime readAt){ public static MessageResponse from(Message m){return new MessageResponse(m.getId(),m.getConversation().getId(),m.getSender().getId(),m.getContent(),m.getImageUrl(),m.getCreatedAt(),m.getReadAt());}}
