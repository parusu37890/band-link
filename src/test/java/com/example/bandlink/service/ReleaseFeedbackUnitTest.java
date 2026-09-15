package com.example.bandlink.service;

import com.example.bandlink.dto.FeedbackRequest;
import com.example.bandlink.entity.*;
import com.example.bandlink.repository.*;
import jakarta.validation.Validation;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

/** Designed only; execution belongs to Runa. IDs map to docs/test-plan/code-tests.md. */
class ReleaseFeedbackUnitTest {
    final UserRepository users = mock(UserRepository.class);
    final FeedbackRepository feedback = mock(FeedbackRepository.class);
    final Clock clock = Clock.fixed(Instant.parse("2026-09-13T03:00:00Z"), ZoneOffset.UTC);
    final FeedbackService service = new FeedbackService(users, feedback, clock);

    @Test void codeUt001_springSelectsInjectableConstructorWithoutClockBean() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(UserRepository.class, () -> users);
            context.registerBean(FeedbackRepository.class, () -> feedback);
            context.register(FeedbackService.class);
            context.refresh();
            when(users.findById(1L)).thenReturn(Optional.of(new User("QA", "qa@example.invalid", "hash")));
            when(feedback.save(any(Feedback.class))).thenAnswer(i -> i.getArgument(0));
            assertEquals("wiring", context.getBean(FeedbackService.class)
                    .create(1L, FeedbackType.CONTACT, new FeedbackRequest("wiring", null)).getMessageText());
        }
    }

    @Test void codeUt002_contactAndFeaturePersistDistinctTypesAndExactTime() {
        User user = new User("QA", "qa@example.invalid", "hash");
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(feedback.save(any(Feedback.class))).thenAnswer(i -> i.getArgument(0));
        for (FeedbackType type : FeedbackType.values()) {
            Feedback saved = service.create(1L, type, new FeedbackRequest("  日本語\n内容  ", "/api/admin/feedback/images/qa-image.png"));
            assertSame(user, saved.getUser());
            assertEquals(type, saved.getType());
            assertEquals("日本語\n内容", saved.getMessageText());
            assertEquals("/api/admin/feedback/images/qa-image.png", saved.getImageUrl());
            assertEquals(LocalDateTime.now(clock), saved.getCreatedAt());
        }
        verify(feedback, times(2)).save(any());
    }

    @Test void codeUt003_blankContentAndMissingTypeHaveNoPersistenceSideEffects() {
        for (String content : new String[]{null, "", " \n\t "}) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.create(1L, FeedbackType.CONTACT, new FeedbackRequest(content, null)));
        }
        assertThrows(IllegalArgumentException.class,
                () -> service.create(1L, null, new FeedbackRequest("valid", null)));
        verifyNoInteractions(users, feedback);
    }

    @Test void codeUt004_externalAndTraversalImagesAreRejectedBeforeSave() {
        // SEC-011: feedback attachments moved off the public /uploads/ tree onto the private
        // /api/admin/feedback/images/ path (ImageStorageService.storeFeedback(),
        // FeedbackController.image()) so a screenshot attached to a contact/feature-request
        // message isn't reachable by anyone who merely has or guesses the URL. The old public
        // shape must now be rejected right alongside the other malformed/foreign values below.
        for (String image : new String[]{"https://example.invalid/a.png", "/uploads/qa-image.png",
                "/api/admin/feedback/images/../a.png", "/api/admin/feedback/images/a.svg",
                "/api/admin/feedback/images/a.png?token=x", "javascript:alert(1)"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.create(1L, FeedbackType.CONTACT, new FeedbackRequest("valid", image)), image);
        }
        verifyNoInteractions(users, feedback);
    }

    @Test void codeUt005_unknownUserCannotLeaveOrphanFeedback() {
        when(users.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service.create(99L, FeedbackType.CONTACT, new FeedbackRequest("valid", null)));
        verifyNoInteractions(feedback);
    }

    @Test void codeUt006_beanValidationEnforces1000CharactersAndImageUrlLimit() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(new FeedbackRequest("あ".repeat(1000), null)).isEmpty());
            assertFalse(validator.validate(new FeedbackRequest("あ".repeat(1001), null)).isEmpty());
            assertFalse(validator.validate(new FeedbackRequest(" ", null)).isEmpty());
            assertFalse(validator.validate(new FeedbackRequest(null, null)).isEmpty());
            assertTrue(validator.validate(new FeedbackRequest("valid", "x".repeat(1000))).isEmpty());
            assertFalse(validator.validate(new FeedbackRequest("valid", "x".repeat(1001))).isEmpty());
        }
    }
}
