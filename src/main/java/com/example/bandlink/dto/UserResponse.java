package com.example.bandlink.dto;

import com.example.bandlink.entity.User;

public record UserResponse(Long id, String username, String email, boolean emailVerified) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.isEmailVerified());
    }
}
