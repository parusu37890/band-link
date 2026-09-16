package com.example.bandlink.service;

import com.example.bandlink.dto.MessageRequests;
import com.example.bandlink.entity.*;
import com.example.bandlink.repository.*;
import jakarta.validation.Validation;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class ReleaseMessageUnitTest {
    final UserRepository users = mock(UserRepository.class);
    final ConversationRepository conversations = mock(ConversationRepository.class);
    final MessageRepository messages = mock(MessageRepository.class);
    final BlockRepository blocks = mock(BlockRepository.class);
    final NotificationRepository notifications = mock(NotificationRepository.class);
    final Clock clock = Clock.fixed(Instant.parse("2026-09-13T03:00:00Z"), ZoneOffset.UTC);
    final MessageService service = new MessageService(users, conversations, messages, blocks, notifications, clock);

    User user(long id) {
        var user = new User("qa" + id, "qa" + id + "@example.invalid", "hash");
        ReflectionTestUtils.setField(user, "id", id);
        user.setEmailVerifiedAt(LocalDateTime.now(clock).minusDays(1));
        when(users.findById(id)).thenReturn(Optional.of(user));
        return user;
    }
    Conversation conversation(User a, User b) {
        var conversation = new Conversation(a, b, LocalDateTime.now(clock).minusHours(1));
        ReflectionTestUtils.setField(conversation, "id", 10L);
        when(conversations.findById(10L)).thenReturn(Optional.of(conversation));
        return conversation;
    }
    void noMessageSideEffects() { verifyNoInteractions(messages, notifications); }

    @Test void codeUt007_unverifiedSenderCannotCreateConversationOrNotification() {
        user(1).setEmailVerifiedAt(null);
        assertThrows(MessageService.RuleViolationException.class,
                () -> service.send(1L, 2L, new MessageRequests.Send("hello", null)));
        verifyNoInteractions(conversations, messages, notifications);
    }
    @Test void codeUt008_inactiveSenderOrRecipientCannotSend() {
        User sender = user(1), recipient = user(2);
        for (UserStatus status : List.of(UserStatus.SUSPENDED, UserStatus.WITHDRAWN)) {
            sender.setStatus(status);
            assertThrows(MessageService.RuleViolationException.class,
                    () -> service.send(1L, 2L, new MessageRequests.Send("hello", null)));
            sender.setStatus(UserStatus.ACTIVE);
            recipient.setStatus(status);
            assertThrows(MessageService.RuleViolationException.class,
                    () -> service.send(1L, 2L, new MessageRequests.Send("hello", null)));
            recipient.setStatus(UserStatus.ACTIVE);
        }
        verifyNoInteractions(conversations, messages, notifications);
    }
    @Test void codeUt009_selfMessagingIsRejected() {
        user(1);
        assertThrows(MessageService.RuleViolationException.class,
                () -> service.send(1L, 1L, new MessageRequests.Send("hello", null)));
        verifyNoInteractions(conversations, messages, notifications);
    }
    @Test void codeUt010_bothDirectionsOfBlockPreventSending() {
        user(1); user(2);
        when(blocks.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(true);
        assertThrows(MessageService.RuleViolationException.class,
                () -> service.send(1L, 2L, new MessageRequests.Send("hello", null)));
        when(blocks.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(false);
        when(blocks.existsByBlockerIdAndBlockedId(2L, 1L)).thenReturn(true);
        assertThrows(MessageService.RuleViolationException.class,
                () -> service.send(1L, 2L, new MessageRequests.Send("hello", null)));
        verifyNoInteractions(conversations, messages, notifications);
    }
    @Test void codeUt011_emptyPayloadAndPublicImageUrlAreRejected() {
        user(1); user(2);
        for (var request : List.of(new MessageRequests.Send(null, null),
                new MessageRequests.Send(" \n ", " "), new MessageRequests.Send("hello", "/uploads/public.png"))) {
            assertThrows(MessageService.RuleViolationException.class, () -> service.send(1L, 2L, request));
        }
        verifyNoInteractions(conversations, messages, notifications);
    }
    @Test void codeUt012_existingConversationIsReusedAndRecipientIsNotified() {
        User sender = user(2), recipient = user(1);
        Conversation c = conversation(sender, recipient);
        when(conversations.findByUserAIdAndUserBId(1L, 2L)).thenReturn(Optional.of(c));
        when(messages.save(any(Message.class))).thenAnswer(i -> i.getArgument(0));
        Message sent = service.send(2L, 1L, new MessageRequests.Send("  hello  ", null));
        assertSame(c, sent.getConversation());
        assertSame(sender, sent.getSender());
        assertEquals("hello", sent.getContent());
        assertEquals(LocalDateTime.now(clock), c.getLastMessageAt());
        verify(conversations, never()).save(any());
        verify(notifications).save(argThat(n -> "NEW_MESSAGE".equals(n.getType()) && n.getRelatedId().equals(10L)));
    }
    @Test void codeUt013_imageOnlyMessageIsAccepted() {
        User sender = user(1), recipient = user(2);
        Conversation c = conversation(sender, recipient);
        when(conversations.findByUserAIdAndUserBId(1L, 2L)).thenReturn(Optional.of(c));
        when(messages.save(any(Message.class))).thenAnswer(i -> i.getArgument(0));
        Message sent = service.send(1L, 2L, new MessageRequests.Send(null, "/api/messages/images/qa.png"));
        assertNull(sent.getContent());
        assertEquals("/api/messages/images/qa.png", sent.getImageUrl());
        verify(notifications).save(any());
    }
    @Test void codeUt014_thirdPartyCannotListReadOrSubscribeToConversation() {
        conversation(user(1), user(2));
        assertThrows(MessageService.RuleViolationException.class, () -> service.messages(3L, 10L));
        assertThrows(MessageService.RuleViolationException.class, () -> service.markRead(3L, 10L));
        assertThrows(MessageService.RuleViolationException.class, () -> service.requireParticipant(3L, 10L));
        noMessageSideEffects();
    }
    @Test void codeUt015_readOnlyUpdatesIncomingUnreadMessagesAndIsIdempotent() {
        User a = user(1), b = user(2);
        Conversation c = conversation(a, b);
        LocalDateTime old = LocalDateTime.now(clock).minusMinutes(10);
        Message incoming = new Message(c, b, "incoming", null, old);
        Message outgoing = new Message(c, a, "outgoing", null, old);
        Message alreadyRead = new Message(c, b, "read", null, old);
        alreadyRead.markRead(old);
        when(messages.findByConversationIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(incoming, outgoing, alreadyRead));
        service.markRead(1L, 10L);
        service.markRead(1L, 10L);
        assertEquals(LocalDateTime.now(clock), incoming.getReadAt());
        assertNull(outgoing.getReadAt());
        assertEquals(old, alreadyRead.getReadAt());
        // Reading the thread also clears the matching NEW_MESSAGE notifications the header's
        // unread badge counts, so it isn't left stuck after the messages themselves are read.
        verify(notifications, times(2)).findByUserIdAndTypeAndRelatedIdAndReadAtIsNull(1L, "NEW_MESSAGE", 10L);
        verify(notifications, never()).saveAll(any());
    }
    @Test void codeUt016_dtoRejects501CharactersButAccepts500() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(new MessageRequests.Send("あ".repeat(500), null)).isEmpty());
            assertFalse(validator.validate(new MessageRequests.Send("あ".repeat(501), null)).isEmpty());
            assertFalse(validator.validate(new MessageRequests.Send(null, "x".repeat(1001))).isEmpty());
        }
    }
}
