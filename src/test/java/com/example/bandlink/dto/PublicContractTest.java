package com.example.bandlink.dto;

import com.example.bandlink.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class PublicContractTest {
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test void publicProfileExposesDecadeButNeverEmailPasswordOrExactAge() {
        User user = user(1L, "Haruki");
        user.setAge(27); user.setBio("ギターを弾いています");
        var json = mapper.readTree(mapper.writeValueAsString(ProfileResponse.from(user)));
        assertEquals("20代", json.get("ageRange").asString());
        assertFalse(json.has("age")); assertFalse(json.has("email")); assertFalse(json.has("passwordHash"));
        assertEquals("ギターを弾いています", json.get("bio").asString());
        var mine = MyProfileResponse.from(user);
        assertEquals(27, mine.age()); assertEquals("private@example.com", mine.email());
    }


    @Test void suspendedPeerIsLabelledAndRestoredWhenTheSuspensionLifts() {
        User viewer = user(1L, "Viewer"), peer = user(2L, "Private Name");
        peer.setProfileImageUrl("/uploads/private.jpg");
        peer.setStatus(UserStatus.SUSPENDED);
        Conversation conversation = new Conversation(viewer, peer, LocalDateTime.now());
        var suspended = ConversationResponse.from(conversation, 1L).otherUser();
        assertEquals("利用停止中ユーザー", suspended.username());
        assertNull(suspended.profileImageUrl(), "a suspended account's photo stays hidden too");
        // Suspension is reversible, so the label must not outlive it (docs/decisions/0001).
        peer.setStatus(UserStatus.ACTIVE);
        var restored = ConversationResponse.from(conversation, 1L).otherUser();
        assertEquals("Private Name", restored.username());
        assertEquals("/uploads/private.jpg", restored.profileImageUrl());
    }
    @Test void conversationUsesPublicPeerAndAnonymizesWithdrawnUser() {
        User viewer = user(1L, "Viewer"), peer = user(2L, "Private Name");
        peer.setStatus(UserStatus.WITHDRAWN);
        peer.setProfileImageUrl("/uploads/private.jpg");
        Conversation conversation = new Conversation(viewer, peer, LocalDateTime.now());
        var dto = ConversationResponse.from(conversation, 1L);
        assertEquals("退会済みユーザー", dto.otherUser().username());
        assertNull(dto.otherUser().profileImageUrl());
        String json = mapper.writeValueAsString(dto);
        assertFalse(json.contains("private@example.com")); assertFalse(json.contains("passwordHash"));
        assertFalse(json.contains("Private Name"));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> ConversationResponse.from(conversation, 3L));
    }

    @Test void listingCarriesThePostersPictureButHidesItWhileTheAccountIsNot() {
        User poster = user(2L, "Haruki");
        poster.setProfileImageUrl("/uploads/haruki.jpg");
        Post post = new Post(poster, PostType.MEMBER_WANTED, "ギター募集", "本文", "中野",
                ActivityFrequency.WEEKLY_1, LocalDateTime.now());

        assertEquals("/uploads/haruki.jpg", PostResponse.from(post).authorImageUrl(),
                "the listing renders initials unless it is given the picture");

        poster.setStatus(UserStatus.SUSPENDED);
        assertNull(PostResponse.from(post).authorImageUrl(), "a suspended account's photo stays hidden");
        poster.setStatus(UserStatus.WITHDRAWN);
        assertNull(PostResponse.from(post).authorImageUrl());
        // Suspension is reversible, so the picture returns with the account (docs/decisions/0001).
        poster.setStatus(UserStatus.ACTIVE);
        assertEquals("/uploads/haruki.jpg", PostResponse.from(post).authorImageUrl());
    }

    private User user(Long id, String name) {
        User user = new User(name, "private@example.com", "secret-hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
