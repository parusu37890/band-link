package com.example.bandlink.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {
    @Test
    void newUserStartsActiveAsNormalUserAndUnverified() {
        User user = new User("haruki", "haruki@example.com", "hashed");

        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(UserRole.USER, user.getRole());
        assertFalse(user.isEmailVerified());
        assertTrue(user.isActive());
        assertTrue(user.getParts().isEmpty());
    }

    @Test
    void emailVerificationAndSuspensionAreReflectedByDomainHelpers() {
        User user = new User("haruki", "haruki@example.com", "hashed");

        user.setEmailVerifiedAt(LocalDateTime.now());
        assertTrue(user.isEmailVerified());

        user.setStatus(UserStatus.SUSPENDED);
        assertFalse(user.isActive());
    }
}
