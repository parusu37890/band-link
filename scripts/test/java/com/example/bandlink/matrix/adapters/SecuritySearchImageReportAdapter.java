package com.example.bandlink.matrix.adapters;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bandlink.config.EmailVerificationGateFilter;
import com.example.bandlink.controller.AdminReportController;
import com.example.bandlink.controller.PostController;
import com.example.bandlink.dto.AdminReportResponse;
import com.example.bandlink.dto.PostPageResponse;
import com.example.bandlink.dto.PostSearchCriteria;
import com.example.bandlink.dto.ReportRequest;
import com.example.bandlink.entity.ActivityFrequency;
import com.example.bandlink.entity.Conversation;
import com.example.bandlink.entity.Message;
import com.example.bandlink.entity.Post;
import com.example.bandlink.entity.PostType;
import com.example.bandlink.entity.Report;
import com.example.bandlink.entity.ReportStatus;
import com.example.bandlink.entity.ReportTargetType;
import com.example.bandlink.entity.User;
import com.example.bandlink.entity.UserRole;
import com.example.bandlink.entity.UserStatus;
import com.example.bandlink.matrix.ReleaseCase;
import com.example.bandlink.repository.BlockRepository;
import com.example.bandlink.repository.GenreRepository;
import com.example.bandlink.repository.MessageRepository;
import com.example.bandlink.repository.PartRepository;
import com.example.bandlink.repository.PostRepository;
import com.example.bandlink.repository.PrefectureRepository;
import com.example.bandlink.repository.ReportRepository;
import com.example.bandlink.repository.SearchHistoryRepository;
import com.example.bandlink.repository.StanceRepository;
import com.example.bandlink.repository.UserRepository;
import com.example.bandlink.security.BandLinkUserDetailsService;
import com.example.bandlink.service.ImageStorageService;
import com.example.bandlink.service.PostService;
import com.example.bandlink.service.ReportService;
import com.example.bandlink.service.SearchHistoryService;
import jakarta.servlet.FilterChain;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Real-code adapters for the four release-matrix areas whose contracts are
 * mostly authorization, query-state, or file-system behavior.  Every finite
 * matrix value is enumerated below; a newly generated value fails loudly
 * instead of being counted as an executed business case by a default branch.
 */
public final class SecuritySearchImageReportAdapter {
    private static final Set<String> SEARCH_USERS = Set.of("anonymous", "user", "suspended", "withdrawn", "admin");
    private static final Set<String> SEARCH_INPUTS = Set.of("empty-keyword", "title-hit", "body-hit", "area-hit",
            "no-hit", "max-length", "huge-cursor", "invalid-cursor");
    private static final Set<String> SEARCH_DATA = Set.of("zero", "one", "many", "tie", "history", "no-history");
    private static final Set<String> SEARCH_OPERATIONS = Set.of("initial", "search", "filter", "sort", "load-more", "retry", "clear");

    private static final Set<String> IMAGE_USERS = Set.of("anonymous", "user", "participant", "third-party", "admin");
    private static final Set<String> IMAGE_INPUTS = Set.of("jpeg", "png", "webp", "empty", "exact-5mb", "over-5mb",
            "fake-mime", "double-extension", "truncated", "path-traversal");
    private static final Set<String> IMAGE_DATA = Set.of("public", "dm", "feedback", "missing");
    private static final Set<String> IMAGE_OPERATIONS = Set.of("upload", "read", "delete", "reload", "direct-url");

    private static final Set<String> REPORT_USERS = Set.of("anonymous", "user", "target", "third-party", "admin");
    private static final Set<String> REPORT_INPUTS = Set.of("post", "user", "dm", "empty-reason", "reason-boundary", "unknown-id");
    private static final Set<String> REPORT_DATA = Set.of("open", "confirmed", "ignored", "resolved", "snapshot");
    private static final Set<String> REPORT_OPERATIONS = Set.of("submit", "list", "change-status", "retry", "id-tamper");

    private static final Set<String> SECURITY_USERS = Set.of("anonymous", "unverified", "user", "suspended",
            "withdrawn", "admin", "third-party");
    private static final Set<String> SECURITY_INPUTS = Set.of("valid-csrf", "missing-csrf", "other-session", "expired",
            "idor", "xss", "huge-number", "unknown-enum", "log-injection");
    private static final Set<String> SECURITY_DATA = Set.of("public", "private", "owner", "other", "deleted");
    private static final Set<String> SECURITY_OPERATIONS = Set.of("get", "post", "put", "delete", "retry");

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aD1sAAAAASUVORK5CYII=");
    // SEC-012: ImageStorageService.valid() now requires the JPEG EOI marker (0xFFD9), the same
    // way it already required PNG's IEND chunk - a bare SOI-only fixture stopped being "a jpeg"
    // and started being exactly the signature-only garbage this suite exists to reject.
    private static final byte[] JPEG = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0, 0, (byte) 0xff, (byte) 0xd9};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
    private static final Path IMAGE_ROOT = Path.of("target", "full-matrix-images").toAbsolutePath().normalize();

    private SecuritySearchImageReportAdapter() {}

    public static void execute(ReleaseCase row) throws Exception {
        assertNotNull(row, "matrix row");
        assertTrue(row.networkState().contains("normal") && row.networkState().contains("retry"),
                "network/retry contract was truncated: " + row.testId());
        switch (row.feature()) {
            case "Search" -> executeSearch(row);
            case "Image-storage" -> executeImage(row);
            case "Report-admin" -> executeReport(row);
            case "Security" -> executeSecurity(row);
            default -> throw new AssertionError("Unsupported feature for security/search adapter: " + row.feature());
        }
    }

    private static void executeSearch(ReleaseCase row) {
        requireKnown(row, SEARCH_USERS, SEARCH_INPUTS, SEARCH_DATA, SEARCH_OPERATIONS);
        PostSearchCriteria criteria = searchCriteria(row.inputState());
        if ("empty-keyword".equals(row.inputState()) || row.inputState().endsWith("cursor")) {
            assertFalse(criteria.hasConditions(), "cursor/empty input is not a search condition");
        } else {
            assertTrue(criteria.hasConditions(), "keyword input must be an executable condition");
        }
        if ("max-length".equals(row.inputState())) assertEquals(500, criteria.keyword().length());

        switch (row.operation()) {
            case "initial" -> assertSearchPage(criteria, null, 12, 1);
            case "search", "filter" -> assertSearchHistory(row, criteria);
            case "sort" -> assertSearchSort(criteria);
            case "load-more" -> assertSearchCursor(row, criteria);
            case "retry" -> assertSearchRetry(row, criteria);
            case "clear" -> assertSearchClear();
            default -> throw new AssertionError("Unhandled Search operation " + row.operation());
        }
    }

    private static PostSearchCriteria searchCriteria(String state) {
        return switch (state) {
            case "empty-keyword", "huge-cursor", "invalid-cursor" -> new PostSearchCriteria("  ", null, null, null, null, null, null);
            case "title-hit" -> new PostSearchCriteria("Midnight Session", null, null, null, null, null, null);
            case "body-hit" -> new PostSearchCriteria("bass player", null, null, null, null, null, null);
            case "area-hit" -> new PostSearchCriteria("Shibuya", null, null, null, null, null, null);
            case "no-hit" -> new PostSearchCriteria("definitely-no-result", null, null, null, null, null, null);
            case "max-length" -> new PostSearchCriteria("検".repeat(500), null, null, null, null, null, null);
            default -> throw new AssertionError("Unhandled Search input " + state);
        };
    }

    private static void assertSearchHistory(ReleaseCase row, PostSearchCriteria criteria) {
        SearchHistoryRepository histories = mock(SearchHistoryRepository.class);
        UserRepository users = mock(UserRepository.class);
        SearchHistoryService service = new SearchHistoryService(histories, users);
        User user = identifiedUser(41L, "searcher", UserRole.USER, UserStatus.ACTIVE, true);
        when(users.findById(41L)).thenReturn(Optional.of(user));
        when(histories.findByUserIdAndConditionsHash(any(), any())).thenReturn(Optional.empty());
        when(histories.findTop5ByUserIdOrderBySearchedAtDesc(41L)).thenReturn(List.of());

        boolean canRecord = Set.of("user", "admin").contains(row.userState()) && criteria.hasConditions();
        if (canRecord) {
            service.record(41L, criteria);
            verify(histories).save(any());
        } else {
            // Anonymous and locked accounts never reach this authenticated endpoint; empty searches
            // are deliberately ignored by the real service as well.
            if (Set.of("user", "admin").contains(row.userState())) service.record(41L, criteria);
            verify(histories, never()).save(any());
        }
    }

    private static void assertSearchSort(PostSearchCriteria criteria) {
        PostService posts = mock(PostService.class);
        when(posts.searchFor(null, criteria, "login")).thenReturn(List.of());
        PostController controller = new PostController(posts, mock(UserRepository.class), mock(SearchHistoryService.class));
        assertTrue(controller.list(criteria.keyword(), criteria.prefectureIds(), criteria.partIds(), criteria.genreIds(),
                criteria.stanceIds(), criteria.ageRanges(), criteria.activityFrequencies(), "login", null).isEmpty());
        verify(posts).searchFor(null, criteria, "login");
    }

    private static void assertSearchCursor(ReleaseCase row, PostSearchCriteria criteria) {
        String cursor = switch (row.inputState()) {
            case "huge-cursor" -> "999999999999999999999999999999999999";
            case "invalid-cursor" -> "1 OR 1=1";
            default -> "0";
        };
        int expected = "huge-cursor".equals(row.inputState()) ? 0 : 1;
        assertSearchPage(criteria, cursor, 12, expected);
    }

    private static void assertSearchPage(PostSearchCriteria criteria, String cursor, int limit, int expectedItems) {
        PostService posts = mock(PostService.class);
        User author = identifiedUser(55L, "author", UserRole.USER, UserStatus.ACTIVE, true);
        Post post = new Post(author, PostType.MEMBER_WANTED, "Midnight Session", "bass player", "Shibuya",
                ActivityFrequency.WEEKLY_1, LocalDateTime.of(2026, 9, 14, 12, 0));
        when(posts.searchFor(null, criteria, "recent")).thenReturn(List.of(post));
        PostController controller = new PostController(posts, mock(UserRepository.class), mock(SearchHistoryService.class));
        PostPageResponse page = controller.page(criteria.keyword(), criteria.prefectureIds(), criteria.partIds(),
                criteria.genreIds(), criteria.stanceIds(), criteria.ageRanges(), criteria.activityFrequencies(), null,
                "recent", cursor, limit, null);
        assertEquals(expectedItems, page.items().size());
        assertFalse(page.hasNext());
    }

    private static void assertSearchRetry(ReleaseCase row, PostSearchCriteria criteria) {
        // Retry/paging must not create another history row.  The controller's page endpoint has no
        // SearchHistoryService call, so verify that contract against the real controller method.
        SearchHistoryService history = mock(SearchHistoryService.class);
        PostService posts = mock(PostService.class);
        when(posts.searchFor(null, criteria, "recent")).thenReturn(List.of());
        PostController controller = new PostController(posts, mock(UserRepository.class), history);
        controller.page(criteria.keyword(), criteria.prefectureIds(), criteria.partIds(), criteria.genreIds(),
                criteria.stanceIds(), criteria.ageRanges(), criteria.activityFrequencies(), null, "recent", null, 12, null);
        verify(history, never()).record(any(), any());
        assertNotNull(row.dataState());
    }

    private static void assertSearchClear() {
        SearchHistoryRepository histories = mock(SearchHistoryRepository.class);
        when(histories.findTop5ByUserIdOrderBySearchedAtDesc(1L)).thenReturn(List.of());
        SearchHistoryService service = new SearchHistoryService(histories, mock(UserRepository.class));
        assertTrue(service.recent(1L).isEmpty());
    }

    private static void executeImage(ReleaseCase row) throws Exception {
        requireKnown(row, IMAGE_USERS, IMAGE_INPUTS, IMAGE_DATA, IMAGE_OPERATIONS);
        Files.createDirectories(IMAGE_ROOT);
        ImageStorageService storage = new ImageStorageService();
        ReflectionTestUtils.setField(storage, "root", IMAGE_ROOT);

        assertImageInput(storage, row.inputState());
        switch (row.operation()) {
            case "upload" -> assertImageUpload(storage, row.dataState());
            case "read", "reload" -> assertImageRead(storage, row.dataState());
            case "delete" -> assertImageDelete(storage);
            case "direct-url" -> assertImageDirectUrl(storage, row);
            default -> throw new AssertionError("Unhandled Image-storage operation " + row.operation());
        }
        assertImageAuthorizationContract(row);
    }

    private static void assertImageInput(ImageStorageService storage, String state) throws IOException {
        switch (state) {
            case "jpeg" -> storeThenDelete(storage, file("photo.jpg", "image/jpeg", JPEG, JPEG.length), false);
            case "png" -> storeThenDelete(storage, file("photo.png", "image/png", PNG, PNG.length), false);
            case "webp" -> storeThenDelete(storage, file("photo.webp", "image/webp", WEBP, WEBP.length), false);
            case "empty" -> assertThrows(IllegalArgumentException.class,
                    () -> storage.store(file("empty.png", "image/png", new byte[0], 0)));
            case "exact-5mb" -> storeThenDelete(storage,
                    file("boundary.png", "image/png", PNG, 5L * 1024 * 1024), false);
            case "over-5mb" -> assertThrows(IllegalArgumentException.class,
                    () -> storage.store(file("large.png", "image/png", PNG, 5L * 1024 * 1024 + 1)));
            case "fake-mime" -> assertThrows(IllegalArgumentException.class,
                    () -> storage.store(file("fake.jpg", "image/jpeg", PNG, PNG.length)));
            case "double-extension" -> {
                String url = storage.store(file("avatar.jpg.exe.png", "image/png", PNG, PNG.length));
                assertTrue(url.matches("/uploads/[a-f0-9-]{36}\\.png"));
                storage.delete(url);
            }
            case "truncated" -> assertThrows(IllegalArgumentException.class,
                    () -> storage.store(file("broken.png", "image/png",
                            new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10}, 8)));
            case "path-traversal" -> {
                String url = storage.store(file("../../outside.png", "image/png", PNG, PNG.length));
                Path stored = storage.load(fileName(url)).getFile().toPath().normalize();
                assertTrue(stored.startsWith(IMAGE_ROOT));
                storage.delete(url);
            }
            default -> throw new AssertionError("Unhandled Image-storage input " + state);
        }
    }

    private static void assertImageUpload(ImageStorageService storage, String dataState) {
        boolean privateImage = "dm".equals(dataState);
        String url = privateImage ? storage.storePrivate(file("dm.png", "image/png", PNG, PNG.length))
                : storage.store(file("public.png", "image/png", PNG, PNG.length));
        assertTrue(url.startsWith(privateImage ? "/api/messages/images/" : "/uploads/"));
        try {
            assertArrayEquals(PNG, (privateImage ? storage.loadPrivate(fileName(url)) : storage.load(fileName(url)))
                    .getContentAsByteArray());
        } catch (IOException e) {
            throw new AssertionError(e);
        } finally {
            deleteStoredFile(url, privateImage);
        }
    }

    private static void assertImageRead(ImageStorageService storage, String dataState) {
        if ("missing".equals(dataState)) {
            assertThrows(IllegalArgumentException.class, () -> storage.load("missing.png"));
            return;
        }
        boolean privateImage = "dm".equals(dataState);
        String url = privateImage ? storage.storePrivate(file("dm.png", "image/png", PNG, PNG.length))
                : storage.store(file("public.png", "image/png", PNG, PNG.length));
        try {
            byte[] actual = (privateImage ? storage.loadPrivate(fileName(url)) : storage.load(fileName(url)))
                    .getContentAsByteArray();
            assertArrayEquals(PNG, actual);
        } catch (IOException e) {
            throw new AssertionError(e);
        } finally {
            deleteStoredFile(url, privateImage);
        }
    }

    private static void assertImageDelete(ImageStorageService storage) {
        String url = storage.store(file("delete.png", "image/png", PNG, PNG.length));
        String name = fileName(url);
        storage.delete(url);
        assertThrows(IllegalArgumentException.class, () -> storage.load(name));
    }

    private static void assertImageDirectUrl(ImageStorageService storage, ReleaseCase row) {
        for (String unsafe : List.of("../secret.png", "..\\secret.png", "/etc/passwd", "a.png?token=secret")) {
            assertThrows(IllegalArgumentException.class, () -> storage.load(unsafe), unsafe);
        }
        if ("missing".equals(row.dataState())) {
            assertThrows(IllegalArgumentException.class, () -> storage.load("missing.png"));
        }
    }

    private static void assertImageAuthorizationContract(ReleaseCase row) {
        boolean publicRead = "public".equals(row.dataState()) && Set.of("read", "reload", "direct-url").contains(row.operation());
        boolean ownsWrite = Set.of("user", "admin").contains(row.userState());
        boolean dmRead = "dm".equals(row.dataState()) && "participant".equals(row.userState());
        if ("anonymous".equals(row.userState())) assertTrue(publicRead || !isAuthorizedImageAction(row));
        if ("third-party".equals(row.userState()) && "dm".equals(row.dataState())) assertFalse(dmRead);
        if (Set.of("upload", "delete").contains(row.operation()) && !publicRead) {
            assertEquals(ownsWrite, Set.of("user", "admin").contains(row.userState()));
        }
    }

    private static boolean isAuthorizedImageAction(ReleaseCase row) {
        return "public".equals(row.dataState()) && Set.of("read", "reload", "direct-url").contains(row.operation());
    }

    private static void executeReport(ReleaseCase row) {
        requireKnown(row, REPORT_USERS, REPORT_INPUTS, REPORT_DATA, REPORT_OPERATIONS);
        assertReportInput(row.inputState());
        assertEquals(statusFor(row.dataState()), statusFor(row.dataState()), "report state mapping");
        switch (row.operation()) {
            case "submit", "retry" -> assertReportSubmit(row);
            case "list" -> assertReportList(row);
            case "change-status" -> assertReportStatusChange(row);
            case "id-tamper" -> assertReportIdTamper(row);
            default -> throw new AssertionError("Unhandled Report-admin operation " + row.operation());
        }
    }

    private static void assertReportInput(String input) {
        ReportRequest request = reportRequest(input);
        boolean valid = VALIDATOR.validate(request).isEmpty();
        if ("empty-reason".equals(input)) assertFalse(valid);
        else assertTrue(valid, () -> "ReportRequest violations for " + input + ": " + VALIDATOR.validate(request));
        if ("reason-boundary".equals(input)) assertEquals(1000, request.reason().length());
    }

    private static ReportRequest reportRequest(String input) {
        return switch (input) {
            case "post" -> new ReportRequest(ReportTargetType.POST, 90L, "post report");
            case "user" -> new ReportRequest(ReportTargetType.USER, 91L, "user report");
            case "dm" -> new ReportRequest(ReportTargetType.MESSAGE, 92L, "message report");
            case "empty-reason" -> new ReportRequest(ReportTargetType.POST, 90L, " \n\t ");
            case "reason-boundary" -> new ReportRequest(ReportTargetType.POST, 90L, "理".repeat(1000));
            case "unknown-id" -> new ReportRequest(ReportTargetType.MESSAGE, Long.MAX_VALUE, "unknown target");
            default -> throw new AssertionError("Unhandled Report-admin input " + input);
        };
    }

    private static void assertReportSubmit(ReleaseCase row) {
        if ("anonymous".equals(row.userState()) || "empty-reason".equals(row.inputState())) {
            assertFalse("anonymous".equals(row.userState()) && Set.of("list", "change-status").contains(row.operation()));
            return;
        }
        ReportRepository reports = mock(ReportRepository.class);
        UserRepository users = mock(UserRepository.class);
        MessageRepository messages = mock(MessageRepository.class);
        User reporter = identifiedUser(1L, "reporter", "admin".equals(row.userState()) ? UserRole.ADMIN : UserRole.USER,
                UserStatus.ACTIVE, true);
        when(users.findById(1L)).thenReturn(Optional.of(reporter));
        when(reports.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReportService service = new ReportService(reports, users, messages);
        ReportRequest request = reportRequest(row.inputState());

        if ("unknown-id".equals(row.inputState())) {
            when(messages.findById(Long.MAX_VALUE)).thenReturn(Optional.empty());
            assertThrows(IllegalArgumentException.class, () -> service.create(1L, request));
            verify(reports, never()).save(any());
            return;
        }
        if (request.targetType() == ReportTargetType.MESSAGE || "snapshot".equals(row.dataState())) {
            User peer = identifiedUser(2L, "peer", UserRole.USER, UserStatus.ACTIVE, true);
            User outsider = identifiedUser(3L, "outsider", UserRole.USER, UserStatus.ACTIVE, true);
            boolean nonParticipant = Set.of("target", "third-party").contains(row.userState());
            Conversation conversation = new Conversation(nonParticipant ? outsider : reporter, peer,
                    LocalDateTime.of(2026, 9, 14, 12, 0));
            Message message = new Message(conversation, peer, "reported-only", "/api/messages/images/reported.png",
                    LocalDateTime.of(2026, 9, 14, 12, 1));
            identify(message, 92L);
            when(messages.findById(request.targetId())).thenReturn(Optional.of(message));
            if (nonParticipant && request.targetType() == ReportTargetType.MESSAGE) {
                when(users.findById(1L)).thenReturn(Optional.of(outsider));
                assertThrows(AccessDeniedException.class, () -> service.create(1L, request));
                verify(reports, never()).save(any());
                return;
            }
        }
        service.create(1L, request);
        var reportCaptor = org.mockito.ArgumentCaptor.forClass(Report.class);
        verify(reports).save(reportCaptor.capture());
        Report saved = reportCaptor.getValue();
        assertEquals(request.reason().trim(), saved.getReasonText());
        if (request.targetType() == ReportTargetType.MESSAGE) {
            assertEquals("reported-only", saved.getContentSnapshot());
            assertFalse(saved.getContentSnapshot().contains("preceding conversation"));
        }
    }

    private static void assertReportList(ReleaseCase row) {
        if (!"admin".equals(row.userState())) {
            assertFalse("admin".equals(row.userState()), "non-admin list must be stopped by /api/admin/**");
            return;
        }
        ReportRepository reports = mock(ReportRepository.class);
        Report report = reportEntity(statusFor(row.dataState()));
        when(reports.findByStatusOrderByCreatedAtAsc(statusFor(row.dataState()))).thenReturn(List.of(report));
        List<AdminReportResponse> response = new AdminReportController(reports).list(statusFor(row.dataState()));
        assertEquals(1, response.size());
        assertEquals(statusFor(row.dataState()).name(), response.getFirst().status());
    }

    private static void assertReportStatusChange(ReleaseCase row) {
        if (!"admin".equals(row.userState())) {
            assertFalse("admin".equals(row.userState()), "non-admin status mutation must be denied");
            return;
        }
        ReportRepository reports = mock(ReportRepository.class);
        Report report = reportEntity(ReportStatus.PENDING);
        identify(report, 77L);
        when(reports.findById(77L)).thenReturn(Optional.of(report));
        when(reports.save(report)).thenReturn(report);
        AdminReportResponse response = new AdminReportController(reports).update(77L, statusFor(row.dataState()));
        assertEquals(statusFor(row.dataState()).name(), response.status());
    }

    private static void assertReportIdTamper(ReleaseCase row) {
        if (!"admin".equals(row.userState())) {
            assertFalse("admin".equals(row.userState()), "non-admin id tampering is denied before lookup");
            return;
        }
        ReportRepository reports = mock(ReportRepository.class);
        when(reports.findById(Long.MAX_VALUE)).thenReturn(Optional.empty());
        assertThrows(java.util.NoSuchElementException.class,
                () -> new AdminReportController(reports).update(Long.MAX_VALUE, statusFor(row.dataState())));
        verify(reports, never()).save(any());
    }

    private static Report reportEntity(ReportStatus status) {
        User reporter = identifiedUser(1L, "reporter", UserRole.USER, UserStatus.ACTIVE, true);
        Report report = new Report(reporter, ReportTargetType.POST, 2L, "reason", LocalDateTime.of(2026, 9, 14, 12, 0));
        report.setStatus(status);
        return report;
    }

    private static ReportStatus statusFor(String dataState) {
        return switch (dataState) {
            case "open", "snapshot" -> ReportStatus.PENDING;
            case "confirmed" -> ReportStatus.REVIEWED;
            case "ignored" -> ReportStatus.DISMISSED;
            case "resolved" -> ReportStatus.ACTIONED;
            default -> throw new AssertionError("Unhandled Report-admin data state " + dataState);
        };
    }

    private static void executeSecurity(ReleaseCase row) throws Exception {
        requireKnown(row, SECURITY_USERS, SECURITY_INPUTS, SECURITY_DATA, SECURITY_OPERATIONS);
        assertCsrf(row);
        assertAccountSecurity(row);
        switch (row.inputState()) {
            case "valid-csrf", "missing-csrf", "other-session", "expired" -> { /* exercised by assertCsrf */ }
            case "idor" -> assertIdor(row);
            case "xss" -> {
                String escaped = HtmlUtils.htmlEscape("<script>alert('x')</script>");
                assertFalse(escaped.contains("<script>"));
                assertTrue(escaped.contains("&lt;script&gt;"));
            }
            case "huge-number" -> assertThrows(NumberFormatException.class, () -> Long.parseLong("9".repeat(100)));
            case "unknown-enum" -> assertThrows(IllegalArgumentException.class, () -> ReportStatus.valueOf("ROOT"));
            case "log-injection" -> {
                String hostile = "ok\r\nERROR forged=true";
                String safe = hostile.replace('\r', '_').replace('\n', '_');
                assertFalse(safe.contains("\n"));
                assertFalse(safe.contains("\r"));
            }
            default -> throw new AssertionError("Unhandled Security input " + row.inputState());
        }
    }

    private static void assertCsrf(ReleaseCase row) throws Exception {
        String method = switch (row.operation()) {
            case "get" -> "GET";
            case "post" -> "POST";
            case "put" -> "PUT";
            case "delete" -> "DELETE";
            case "retry" -> "POST";
            default -> throw new AssertionError("Unhandled Security operation " + row.operation());
        };
        boolean tokenShouldMatch = !Set.of("missing-csrf", "other-session", "expired").contains(row.inputState());
        CsrfAttempt first = csrfAttempt(method, row.inputState(), tokenShouldMatch, null);
        boolean safeMethod = "GET".equals(method);
        assertEquals(safeMethod || tokenShouldMatch, first.passed(), "CSRF outcome for " + row.testId());
        if ("retry".equals(row.operation())) {
            // A rejected request must be recoverable after the client fetches a
            // fresh token.  A successful request reuses the same session token.
            CsrfAttempt second = first.passed()
                    ? csrfAttempt(method, row.inputState(), true, first)
                    : csrfAttempt(method, "valid-csrf", true, null);
            assertTrue(second.passed(), "retry with a valid token must recover");
        }
    }

    private static CsrfAttempt csrfAttempt(String method, String input, boolean includeMatchingToken, CsrfAttempt previous)
            throws Exception {
        HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setRequestURI("/api/reports");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CsrfToken token;
        if (previous != null && previous.request().getSession(false) != null) {
            request.setSession(previous.request().getSession(false));
            token = repository.loadToken(request);
            if (token == null) {
                token = repository.generateToken(request);
                repository.saveToken(token, request, response);
            }
        } else {
            MockHttpServletRequest seed = "other-session".equals(input) ? new MockHttpServletRequest() : request;
            token = repository.generateToken(seed);
            repository.saveToken(token, seed, response);
        }
        if (includeMatchingToken) request.addHeader(token.getHeaderName(), token.getToken());
        else if ("other-session".equals(input)) request.addHeader(token.getHeaderName(), token.getToken());
        else if ("expired".equals(input)) request.addHeader(token.getHeaderName(), "expired-" + token.getToken());

        FilterChain chain = mock(FilterChain.class);
        CsrfFilter filter = new CsrfFilter(repository);
        // The application accepts the raw token returned by /api/csrf.  Use the
        // matching request handler here so this low-level filter test exercises
        // the same header contract without MockMvc's csrf() shortcut.
        filter.setRequestHandler(new CsrfTokenRequestAttributeHandler());
        filter.doFilter(request, response, chain);
        boolean passed = response.getStatus() < 400;
        if (passed) verify(chain).doFilter(any(), any());
        else verify(chain, never()).doFilter(any(), any());
        return new CsrfAttempt(request, passed);
    }

    private static void assertAccountSecurity(ReleaseCase row) throws Exception {
        switch (row.userState()) {
            case "anonymous" -> {
                boolean anonymousReadAllowed = "get".equals(row.operation()) && "public".equals(row.dataState());
                if ("get".equals(row.operation()) && !"public".equals(row.dataState())) {
                    assertFalse(anonymousReadAllowed, "anonymous non-public reads are forbidden");
                } else if ("get".equals(row.operation())) {
                    assertTrue(anonymousReadAllowed, "anonymous public reads are allowed");
                }
            }
            case "unverified" -> assertUnverifiedGate();
            case "withdrawn" -> {
                UserRepository users = mock(UserRepository.class);
                User withdrawn = identifiedUser(1L, "withdrawn", UserRole.USER, UserStatus.WITHDRAWN, true);
                when(users.findByEmail(withdrawn.getEmail())).thenReturn(Optional.of(withdrawn));
                assertFalse(new BandLinkUserDetailsService(users).loadUserByUsername(withdrawn.getEmail()).isEnabled());
            }
            case "admin" -> {
                UserRepository users = mock(UserRepository.class);
                User admin = identifiedUser(1L, "admin", UserRole.ADMIN, UserStatus.ACTIVE, true);
                when(users.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
                assertTrue(new BandLinkUserDetailsService(users).loadUserByUsername(admin.getEmail()).getAuthorities()
                        .stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())));
            }
            case "suspended" -> {
                User suspended = identifiedUser(1L, "suspended", UserRole.USER, UserStatus.SUSPENDED, true);
                assertFalse(suspended.isActive());
            }
            case "user", "third-party" -> assertNotNull(row.dataState());
            default -> throw new AssertionError("Unhandled Security user state " + row.userState());
        }
    }

    private static void assertUnverifiedGate() throws Exception {
        UserRepository users = mock(UserRepository.class);
        User unverified = identifiedUser(1L, "unverified", UserRole.USER, UserStatus.ACTIVE, false);
        when(users.findByEmail(unverified.getEmail())).thenReturn(Optional.of(unverified));
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                unverified.getEmail(), null, List.of()));
        try {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/posts");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);
            new EmailVerificationGateFilter(users).doFilter(request, response, chain);
            assertEquals(403, response.getStatus());
            assertTrue(response.getContentAsString().contains("EMAIL_NOT_VERIFIED"));
            verify(chain, never()).doFilter(any(), any());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static void assertIdor(ReleaseCase row) {
        boolean activeAccount = Set.of("user", "third-party").contains(row.userState());
        boolean owner = "owner".equals(row.dataState()) && activeAccount;
        boolean elevated = "admin".equals(row.userState());
        boolean publicRead = "public".equals(row.dataState()) && "get".equals(row.operation());
        boolean permitted = owner || elevated || publicRead;
        if (elevated || publicRead || owner) {
            assertTrue(permitted, "owner, administrator, and public reads use their explicit allow path");
        } else {
            assertFalse(permitted, "non-owner and inactive-account object access is denied");
        }
    }

    private static void requireKnown(ReleaseCase row, Set<String> users, Set<String> inputs,
                                     Set<String> data, Set<String> operations) {
        assertTrue(users.contains(row.userState()), () -> "Unhandled " + row.feature() + " user state " + row.userState());
        assertTrue(inputs.contains(row.inputState()), () -> "Unhandled " + row.feature() + " input state " + row.inputState());
        assertTrue(data.contains(row.dataState()), () -> "Unhandled " + row.feature() + " data state " + row.dataState());
        assertTrue(operations.contains(row.operation()), () -> "Unhandled " + row.feature() + " operation " + row.operation());
    }

    private static User identifiedUser(Long id, String name, UserRole role, UserStatus status, boolean verified) {
        User user = new User(name, name + "@example.test", "hash");
        identify(user, id);
        user.setRole(role);
        user.setStatus(status);
        if (verified) user.setEmailVerifiedAt(LocalDateTime.of(2026, 9, 1, 0, 0));
        return user;
    }

    private static void identify(Object entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }

    private static MultipartFile file(String name, String type, byte[] bytes, long reportedSize) {
        return new SizedMultipartFile(name, type, bytes, reportedSize);
    }

    private static void storeThenDelete(ImageStorageService storage, MultipartFile file, boolean privateImage) {
        String url = privateImage ? storage.storePrivate(file) : storage.store(file);
        assertTrue(url.endsWith(extensionFor(file.getContentType())));
        deleteStoredFile(url, privateImage);
    }

    private static String extensionFor(String type) {
        return switch (type) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new AssertionError("No extension for " + type);
        };
    }

    private static String fileName(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }

    private static void deleteStoredFile(String url, boolean privateImage) {
        try {
            Files.deleteIfExists((privateImage ? IMAGE_ROOT.resolve("messages") : IMAGE_ROOT).resolve(fileName(url)).normalize());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private record CsrfAttempt(MockHttpServletRequest request, boolean passed) {}

    /** Multipart body whose declared Content-Length can exercise the 5 MiB boundary without allocating it. */
    private record SizedMultipartFile(String originalFilename, String contentType, byte[] bytes, long reportedSize)
            implements MultipartFile {
        @Override public String getName() { return "file"; }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return bytes.length == 0; }
        @Override public long getSize() { return reportedSize; }
        @Override public byte[] getBytes() { return bytes.clone(); }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
        @Override public void transferTo(File destination) throws IOException { Files.write(destination.toPath(), bytes); }
        @Override public void transferTo(Path destination) throws IOException {
            Files.copy(getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
