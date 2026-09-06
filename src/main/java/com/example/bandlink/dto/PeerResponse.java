package com.example.bandlink.dto;

import com.example.bandlink.entity.User;
import com.example.bandlink.entity.UserStatus;

public record PeerResponse(Long id, String username, String profileImageUrl, String status) {
    /**
     * Withdrawn and suspended accounts are both hidden behind a state label (docs/decisions/0001).
     * The label is derived from the current status rather than stored, so lifting a suspension
     * restores the real name — withdrawal is one-way, suspension is not.
     */
    public static PeerResponse from(User user) {
        String label = switch (user.getStatus()) {
            case WITHDRAWN -> "退会済みユーザー";
            case SUSPENDED -> "利用停止中ユーザー";
            default -> user.getUsername();
        };
        boolean hidden = user.getStatus() != UserStatus.ACTIVE;
        return new PeerResponse(user.getId(), label, hidden ? null : user.getProfileImageUrl(), user.getStatus().name());
    }
}
