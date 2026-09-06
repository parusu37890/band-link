package com.example.bandlink.dto;

import jakarta.validation.constraints.Size;

public final class MessageRequests {
    private MessageRequests() {}
    public record Send(@Size(max=1000) String content, @Size(max=1000) String imageUrl) {}
}
