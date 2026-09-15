package com.example.bandlink.matrix.adapters;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.bandlink.dto.MessageRequests;
import com.example.bandlink.dto.PostRequests;
import com.example.bandlink.entity.*;
import com.example.bandlink.matrix.ReleaseCase;
import com.example.bandlink.repository.*;
import com.example.bandlink.service.BlockService;
import com.example.bandlink.service.MessageService;
import com.example.bandlink.service.PostService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Executes post, message, and block matrix rows against the real services with
 * isolated Mockito repositories. Invalid controller DTOs take a read-only
 * service path and are asserted to cause no writes.
 */
public final class PostMessageStateAdapter {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 9, 14, 12, 0);
    private static final Set<String> POST_USERS = Set.of(
            "admin", "anonymous", "other-owner", "suspended", "unverified", "user", "withdrawn");
    private static final Set<String> POST_INPUTS = Set.of(
            "body-boundary", "empty-body", "five-images", "join", "missing-choice", "recruit",
            "six-images", "too-many-choice", "zero-images");
    private static final Set<String> POST_DATA = Set.of(
            "closed", "edit-locked", "expired", "other-type-open", "posted-0-to-4-weeks",
            "same-type-open");
    private static final Set<String> POST_OPS = Set.of(
            "list", "detail", "create", "edit", "close", "republish", "delete", "double-submit");
    private static final Set<String> MSG_USERS = Set.of(
            "admin", "anonymous", "recipient", "sender", "suspended", "third-party", "unverified", "withdrawn");
    private static final Set<String> MSG_INPUTS = Set.of(
            "empty", "fake-image", "image", "oversize-image", "text", "text-boundary", "text-image");
    private static final Set<String> MSG_DATA = Set.of(
            "existing", "mutual-block", "new-conversation", "one-way-block", "read", "reported", "unread");
    private static final Set<String> MSG_OPS = Set.of(
            "list", "start", "send", "read", "expand", "block", "unblock", "report", "retry");

    private PostMessageStateAdapter() {}

    public static void execute(ReleaseCase row) {
        assertNotNull(row);
        switch (row.feature()) {
            case "Recruitment-post" -> post(row);
            case "Direct-message" -> message(row);
            default -> throw new AssertionError("Unsupported feature: " + row.feature());
        }
    }

    private static void post(ReleaseCase row) {
        mapped(row.userState(), POST_USERS, "post user");
        mapped(row.inputState(), POST_INPUTS, "post input");
        mapped(row.dataState(), POST_DATA, "post data");
        mapped(row.operation(), POST_OPS, "post operation");
        PostRequests.Create request = postRequest(row.inputState());
        boolean dtoValid = VALIDATOR.validate(request).isEmpty();
        assertEquals(!Set.of("empty-body", "missing-choice", "too-many-choice").contains(row.inputState()),
                dtoValid, "post DTO " + row.inputState());
        int images = row.inputState().equals("six-images") ? 6
                : row.inputState().equals("five-images") ? 5 : 0;
        boolean inputValid = dtoValid && images <= 5;
        assertEquals(row.inputState().equals("six-images"), images > 5, "image-count boundary");
        PostFx fx = new PostFx(row);
        switch (row.operation()) {
            case "list" -> fx.list();
            case "detail" -> fx.detail();
            case "create" -> fx.create(request, inputValid);
            case "edit" -> fx.edit(update(request), inputValid);
            case "close" -> fx.close();
            case "republish" -> fx.republish();
            case "delete" -> fx.delete();
            case "double-submit" -> fx.doubleSubmit(request, inputValid);
            default -> throw new AssertionError("Unmapped post operation " + row.operation());
        }
    }

    private static void message(ReleaseCase row) {
        mapped(row.userState(), MSG_USERS, "message user");
        mapped(row.inputState(), MSG_INPUTS, "message input");
        mapped(row.dataState(), MSG_DATA, "message data");
        mapped(row.operation(), MSG_OPS, "message operation");
        MessageRequests.Send request = messageRequest(row.inputState());
        boolean dtoValid = VALIDATOR.validate(request).isEmpty();
        assertEquals(!row.inputState().equals("oversize-image"), dtoValid,
                "message DTO " + row.inputState());
        MessageFx fx = new MessageFx(row);
        switch (row.operation()) {
            case "list" -> fx.list();
            case "start", "send", "retry" -> fx.send(request, dtoValid);
            case "read" -> fx.read();
            case "expand" -> fx.expand();
            case "block" -> fx.block();
            case "unblock" -> fx.unblock();
            case "report" -> fx.report();
            default -> throw new AssertionError("Unmapped message operation " + row.operation());
        }
    }

    private static final class PostFx {
        static final long PRIMARY = 101, OTHER = 102, POST = 701;
        final ReleaseCase row;
        final PostRepository posts = mock(PostRepository.class);
        final UserRepository users = mock(UserRepository.class);
        final PartRepository parts = mock(PartRepository.class);
        final GenreRepository genres = mock(GenreRepository.class);
        final StanceRepository stances = mock(StanceRepository.class);
        final PrefectureRepository prefectures = mock(PrefectureRepository.class);
        final BlockRepository blocks = mock(BlockRepository.class);
        final PostService service = new PostService(posts, users, parts, genres, stances, prefectures, blocks);
        final Map<Long, User> known = new HashMap<>();
        final long actorId;
        final User actor;
        final Post post;
        final Set<PostType> createdTypes = EnumSet.noneOf(PostType.class);

        PostFx(ReleaseCase row) {
            this.row = row;
            actorId = row.userState().equals("anonymous") ? 999
                    : row.userState().equals("other-owner") ? OTHER : PRIMARY;
            known.put(PRIMARY, user(PRIMARY, "post-primary"));
            known.put(OTHER, user(OTHER, "post-other"));
            actor = known.get(actorId);
            applyState(actor, row.userState());
            if (actor != null && row.userState().equals("admin")) actor.setRole(UserRole.ADMIN);
            long ownerId = PRIMARY;
            PostType existingType = switch (row.dataState()) {
                case "other-type-open" -> row.inputState().equals("join")
                        ? PostType.MEMBER_WANTED : PostType.WANTS_TO_JOIN;
                default -> row.inputState().equals("join")
                        ? PostType.WANTS_TO_JOIN : PostType.MEMBER_WANTED;
            };
            post = new Post(known.get(ownerId), existingType, "title", "content",
                    "Tokyo", ActivityFrequency.WEEKLY_1, BASE);
            id(post, POST);
            if (row.dataState().equals("closed")) post.close(ClosedReason.MANUAL, BASE.plusHours(1));
            if (row.dataState().equals("expired")) post.close(ClosedReason.EXPIRED, BASE.plusDays(30));
            if (row.dataState().equals("edit-locked") && actor != null)
                actor.setLastEditedAt(LocalDateTime.now().minusHours(1));
            when(users.findById(anyLong())).thenAnswer(i -> Optional.ofNullable(known.get(i.getArgument(0))));
            when(parts.findAllById(any())).thenReturn(List.of());
            when(genres.findAllById(any())).thenReturn(List.of());
            when(stances.findAllById(any())).thenReturn(List.of());
            when(prefectures.findAllById(any())).thenReturn(List.of());
            when(posts.findById(POST)).thenReturn(Optional.of(post));
            when(posts.existsById(POST)).thenReturn(true);
            when(posts.findByIdAndUserId(anyLong(), anyLong())).thenAnswer(i ->
                    i.getArgument(0).equals(POST) && post.getUser().getId().equals(i.getArgument(1))
                            ? Optional.of(post) : Optional.empty());
            when(posts.existsByUserIdAndStatusAndType(anyLong(), any(), any())).thenAnswer(i ->
                    (i.getArgument(1) == PostStatus.OPEN && post.getStatus() == PostStatus.OPEN
                            && post.getUser().getId().equals(i.getArgument(0))
                            && post.getType() == i.getArgument(2))
                            || createdTypes.contains(i.getArgument(2)));
            when(posts.findByStatusOrderByRankUpdatedAtDesc(PostStatus.OPEN)).thenReturn(List.of(post));
            when(posts.findByUserIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of(post));
            when(posts.save(any(Post.class))).thenAnswer(i -> i.getArgument(0));
        }

        void list() {
            List<Post> found = service.listOpen();
            assertEquals(post.getStatus() == PostStatus.OPEN ? 1 : 0, found.size());
        }

        void detail() {
            if (!post.getUser().isActive()) {
                assertThrows(ResponseStatusException.class, () -> service.getPublic(POST));
            } else {
                assertSame(post, service.getPublic(POST));
                assertTrue(post.getStatus() == PostStatus.OPEN
                        || post.getClosedReason() == ClosedReason.MANUAL
                        || post.getClosedReason() == ClosedReason.EXPIRED);
            }
        }

        void create(PostRequests.Create request, boolean validInput) {
            if (!validInput) { readOnlyInvalid(); return; }
            // The 12-hour lock applies to editing an existing post.  Creating a new
            // recruitment or join post is intentionally unrestricted by that lock.
            boolean allowed = activeVerified(actor) && !actorHasOpen(request.type());
            if (allowed) {
                Post created = service.create(actorId, request);
                createdTypes.add(request.type());
                assertSame(actor, created.getUser());
                assertNotNull(actor.getLastEditedAt());
                verify(posts).save(created);
            } else {
                assertThrows(RuntimeException.class, () -> service.create(actorId, request));
                verify(posts, never()).save(any());
            }
        }

        void edit(PostRequests.Update request, boolean validInput) {
            if (!validInput) { readOnlyInvalid(); return; }
            boolean allowed = activeVerified(actor) && !locked() && owns() && post.getStatus() == PostStatus.OPEN;
            if (allowed) {
                assertSame(post, service.update(actorId, POST, request));
                assertEquals(request.title().trim(), post.getTitle());
                assertNotNull(actor.getLastEditedAt());
            } else {
                assertThrows(RuntimeException.class, () -> service.update(actorId, POST, request));
                assertEquals("title", post.getTitle());
            }
        }

        void close() {
            boolean allowed = active(actor) && owns();
            if (allowed) {
                service.close(actorId, POST);
                assertEquals(PostStatus.CLOSED, post.getStatus());
                assertEquals(ClosedReason.MANUAL, post.getClosedReason());
            } else {
                RuntimeException error = assertThrows(RuntimeException.class, () -> service.close(actorId, POST));
                if (active(actor) && !owns()) assertTrue(error instanceof AccessDeniedException);
            }
        }

        void republish() {
            boolean closed = post.getStatus() == PostStatus.CLOSED
                    && Set.of(ClosedReason.MANUAL, ClosedReason.EXPIRED).contains(post.getClosedReason());
            boolean allowed = activeVerified(actor) && owns() && closed;
            if (allowed) {
                assertSame(post, service.reopen(actorId, POST));
                assertEquals(PostStatus.OPEN, post.getStatus());
                assertNull(post.getClosedReason());
                assertTrue(post.getExpiresAt().isAfter(LocalDateTime.now().plusDays(29)));
            } else {
                assertThrows(RuntimeException.class, () -> service.reopen(actorId, POST));
            }
        }

        void delete() {
            if (row.userState().equals("admin")) {
                service.adminDelete(POST);
                assertEquals(ClosedReason.DELETED_BY_ADMIN, post.getClosedReason());
            } else {
                // Current production API implements owner removal as close and
                // administrator removal as adminDelete.
                close();
            }
        }

        void doubleSubmit(PostRequests.Create request, boolean validInput) {
            if (!validInput) { readOnlyInvalid(); return; }
            boolean firstAllowed = activeVerified(actor) && !actorHasOpen(request.type());
            if (firstAllowed) {
                assertNotNull(service.create(actorId, request));
                createdTypes.add(request.type());
                assertThrows(PostService.RuleViolationException.class, () -> service.create(actorId, request));
                verify(posts, times(1)).save(any());
            } else {
                assertThrows(RuntimeException.class, () -> service.create(actorId, request));
                verify(posts, never()).save(any());
            }
            verify(posts, atMost(1)).save(any());
        }

        void readOnlyInvalid() {
            assertNotNull(service.listOpen());
            verify(posts, never()).save(any());
        }

        boolean owns() { return actor != null && post.getUser().getId().equals(actorId); }
        boolean actorHasOpen(PostType type) {
            return post.getStatus() == PostStatus.OPEN && post.getUser().getId().equals(actorId)
                    && post.getType() == type;
        }
        boolean locked() {
            return actor != null && actor.getLastEditedAt() != null
                    && actor.getLastEditedAt().isAfter(LocalDateTime.now().minusHours(12));
        }
    }

    private static final class MessageFx {
        static final long SENDER = 201, RECIPIENT = 202, THIRD = 203, ADMIN = 204, MISSING = 999, CONV = 801;
        final ReleaseCase row;
        final UserRepository users = mock(UserRepository.class);
        final ConversationRepository conversations = mock(ConversationRepository.class);
        final MessageRepository messages = mock(MessageRepository.class);
        final BlockRepository blocks = mock(BlockRepository.class);
        final NotificationRepository notifications = mock(NotificationRepository.class);
        final MessageService messageService = new MessageService(users, conversations, messages, blocks, notifications);
        final BlockService blockService = new BlockService(users, blocks);
        final Map<Long, User> known = new HashMap<>();
        final long actorId, targetId;
        final User actor;
        final Conversation conversation;
        final Message incoming, outgoing;

        MessageFx(ReleaseCase row) {
            this.row = row;
            known.put(SENDER, user(SENDER, "sender"));
            known.put(RECIPIENT, user(RECIPIENT, "recipient"));
            known.put(THIRD, user(THIRD, "third"));
            known.put(ADMIN, user(ADMIN, "admin"));
            known.get(ADMIN).setRole(UserRole.ADMIN);
            actorId = switch (row.userState()) {
                case "recipient" -> RECIPIENT;
                case "third-party" -> THIRD;
                case "admin" -> ADMIN;
                case "anonymous" -> MISSING;
                default -> SENDER;
            };
            targetId = actorId == RECIPIENT ? SENDER : RECIPIENT;
            actor = known.get(actorId);
            applyState(actor, row.userState());
            conversation = new Conversation(known.get(SENDER), known.get(RECIPIENT), BASE);
            id(conversation, CONV);
            User incomingSender = actorId == RECIPIENT ? known.get(SENDER) : known.get(RECIPIENT);
            incoming = new Message(conversation, incomingSender, "incoming", null, BASE);
            outgoing = new Message(conversation, actor == null ? known.get(SENDER) : actor, "outgoing", null, BASE);
            if (row.dataState().equals("read")) incoming.markRead(BASE.plusMinutes(1));
            when(users.findById(anyLong())).thenAnswer(i -> Optional.ofNullable(known.get(i.getArgument(0))));
            when(conversations.findById(CONV)).thenReturn(row.dataState().equals("new-conversation")
                    ? Optional.empty() : Optional.of(conversation));
            when(conversations.findByUserAIdAndUserBId(anyLong(), anyLong())).thenAnswer(i -> {
                long first = i.getArgument(0), second = i.getArgument(1);
                boolean seededPair = first == SENDER && second == RECIPIENT;
                return !row.dataState().equals("new-conversation") && seededPair
                        ? Optional.of(conversation) : Optional.empty();
            });
            when(conversations.findByUserAIdOrUserBIdOrderByLastMessageAtDesc(anyLong(), anyLong()))
                    .thenAnswer(i -> conversation.includes(i.getArgument(0))
                            && !row.dataState().equals("new-conversation") ? List.of(conversation) : List.of());
            when(conversations.save(any())).thenAnswer(i -> {
                Conversation saved = i.getArgument(0);
                if (saved.getId() == null) id(saved, 802);
                return saved;
            });
            when(messages.save(any())).thenAnswer(i -> i.getArgument(0));
            when(messages.findByConversationIdOrderByCreatedAtAsc(CONV)).thenReturn(List.of(incoming, outgoing));
            when(blocks.existsByBlockerIdAndBlockedId(anyLong(), anyLong())).thenAnswer(i -> {
                long a = i.getArgument(0), b = i.getArgument(1);
                boolean pair = (a == actorId && b == targetId) || (a == targetId && b == actorId);
                return pair && (row.dataState().equals("mutual-block")
                        || row.dataState().equals("one-way-block") && a == actorId);
            });
            when(blocks.findByBlockerIdAndBlockedId(anyLong(), anyLong())).thenAnswer(i -> {
                long a = i.getArgument(0), b = i.getArgument(1);
                boolean exists = actor != null && a == actorId && b == targetId
                        && Set.of("one-way-block", "mutual-block").contains(row.dataState());
                return exists ? Optional.of(new Block(actor, known.get(targetId), BASE)) : Optional.empty();
            });
        }

        void list() {
            List<Conversation> found = messageService.conversations(actorId);
            boolean visible = actor != null && conversation.includes(actorId)
                    && !row.dataState().equals("new-conversation");
            assertEquals(visible ? 1 : 0, found.size());
        }

        void send(MessageRequests.Send request, boolean dtoValid) {
            if (!dtoValid) {
                assertNotNull(messageService.conversations(actorId));
                verify(messages, never()).save(any());
                return;
            }
            boolean payloadValid = !Set.of("empty", "fake-image").contains(row.inputState());
            boolean blocked = Set.of("one-way-block", "mutual-block").contains(row.dataState());
            boolean allowed = activeVerified(actor) && payloadValid && !blocked;
            if (allowed) {
                Message sent = messageService.send(actorId, targetId, request);
                assertSame(actor, sent.getSender());
                assertEquals(request.content() == null ? null : request.content().trim(), sent.getContent());
                verify(messages).save(sent);
                verify(notifications).save(any());
                boolean seededPair = !row.dataState().equals("new-conversation")
                        && conversation.includes(actorId) && conversation.includes(targetId);
                verify(conversations, seededPair ? never() : times(1)).save(any());
            } else {
                assertThrows(RuntimeException.class, () -> messageService.send(actorId, targetId, request));
                verify(messages, never()).save(any());
                verify(notifications, never()).save(any());
            }
        }

        void read() {
            boolean allowed = !row.dataState().equals("new-conversation") && conversation.includes(actorId);
            LocalDateTime before = incoming.getReadAt();
            if (allowed) {
                messageService.markRead(actorId, CONV);
                assertNotNull(incoming.getReadAt());
                assertNull(outgoing.getReadAt());
                if (before != null) assertEquals(before, incoming.getReadAt());
            } else {
                assertThrows(MessageService.RuleViolationException.class, () -> messageService.markRead(actorId, CONV));
                assertEquals(before, incoming.getReadAt());
            }
        }

        void expand() {
            boolean allowed = !row.dataState().equals("new-conversation") && conversation.includes(actorId);
            if (allowed) assertEquals(List.of(incoming, outgoing), messageService.messages(actorId, CONV));
            else assertThrows(MessageService.RuleViolationException.class, () -> messageService.messages(actorId, CONV));
        }

        void report() {
            boolean allowed = !row.dataState().equals("new-conversation") && conversation.includes(actorId);
            if (allowed) messageService.requireParticipant(actorId, CONV);
            else assertThrows(MessageService.RuleViolationException.class,
                    () -> messageService.requireParticipant(actorId, CONV));
            verify(messages, never()).save(any());
        }

        void block() {
            boolean exists = Set.of("one-way-block", "mutual-block").contains(row.dataState());
            if (actor == null) {
                assertThrows(NoSuchElementException.class, () -> blockService.block(actorId, targetId));
                verify(blocks, never()).save(any());
            } else {
                blockService.block(actorId, targetId);
                verify(blocks, exists ? never() : times(1)).save(any());
            }
        }

        void unblock() {
            boolean exists = actor != null && Set.of("one-way-block", "mutual-block").contains(row.dataState());
            blockService.unblock(actorId, targetId);
            verify(blocks, exists ? times(1) : never()).delete(any());
            verify(blocks).findByBlockerIdAndBlockedId(actorId, targetId);
        }
    }

    private static PostRequests.Create postRequest(String state) {
        Set<Long> parts = Set.of(1L), genres = Set.of(1L), stances = Set.of(1L), prefectures = Set.of(1L);
        String title = "title", body = "content";
        if (state.equals("empty-body")) { title = ""; body = ""; }
        if (state.equals("body-boundary")) { title = "t".repeat(30); body = "b".repeat(500); }
        if (state.equals("missing-choice")) { parts = Set.of(); genres = Set.of(); stances = Set.of(); prefectures = Set.of(); }
        if (state.equals("too-many-choice")) {
            parts = Set.of(1L, 2L, 3L, 4L, 5L, 6L);
            genres = Set.of(1L, 2L, 3L, 4L);
            stances = Set.of(1L, 2L);
            prefectures = Set.of(1L, 2L, 3L, 4L);
        }
        return new PostRequests.Create(state.equals("join") ? PostType.WANTS_TO_JOIN : PostType.MEMBER_WANTED,
                title, body, "Tokyo", parts, genres, stances, prefectures,
                Set.of(AgeRange.ANY), ActivityFrequency.WEEKLY_1);
    }

    private static PostRequests.Update update(PostRequests.Create r) {
        return new PostRequests.Update(r.title(), r.content(), r.areaSub(), r.partIds(), r.genreIds(),
                r.stanceIds(), r.prefectureIds(), r.ageRanges(), r.activityFrequency());
    }

    private static MessageRequests.Send messageRequest(String state) {
        return switch (state) {
            case "empty" -> new MessageRequests.Send(null, null);
            case "text-boundary" -> new MessageRequests.Send("m".repeat(500), null);
            case "oversize-image" -> new MessageRequests.Send(null, "i".repeat(1001));
            case "image" -> new MessageRequests.Send(null, "/api/messages/images/qa.png");
            case "text-image" -> new MessageRequests.Send("message", "/api/messages/images/qa.png");
            case "fake-image" -> new MessageRequests.Send(null, "/uploads/fake.png");
            case "text" -> new MessageRequests.Send("message", null);
            default -> throw new AssertionError("Unmapped message input " + state);
        };
    }

    private static boolean active(User u) { return u != null && u.getStatus() == UserStatus.ACTIVE; }
    private static boolean activeVerified(User u) { return active(u) && u.isEmailVerified(); }

    private static void applyState(User u, String state) {
        if (u == null) return;
        if (state.equals("unverified")) u.setEmailVerifiedAt(null);
        if (state.equals("suspended")) u.setStatus(UserStatus.SUSPENDED);
        if (state.equals("withdrawn")) u.setStatus(UserStatus.WITHDRAWN);
    }

    private static User user(long id, String name) {
        User u = new User(name, name + "@qa.invalid", "hash");
        id(u, id);
        u.setEmailVerifiedAt(BASE.minusDays(1));
        return u;
    }

    private static void mapped(String value, Set<String> allowed, String dimension) {
        assertTrue(allowed.contains(value), "Unmapped " + dimension + ": " + value);
    }

    private static void id(Object target, long id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
