package com.example.bandlink.dto;

import com.example.bandlink.entity.User;
import com.example.bandlink.entity.UserStatus;

public record PeerResponse(Long id, String username, String profileImageUrl, String status) {
    public static PeerResponse from(User user) {
        boolean withdrawn = user.getStatus() == UserStatus.WITHDRAWN;
        return new PeerResponse(user.getId(), withdrawn ? "退会済みユーザー" : user.getUsername(),
                withdrawn ? null : user.getProfileImageUrl(), user.getStatus().name());
    }
}
