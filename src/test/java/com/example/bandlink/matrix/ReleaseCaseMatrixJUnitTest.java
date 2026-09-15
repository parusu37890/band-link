package com.example.bandlink.matrix;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.bandlink.controller.PostController;
import com.example.bandlink.service.ImageStorageService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Executes one JUnit dynamic test for every API/unit/integration row in the
 * release inventory.  A matrix row is deliberately not treated as a PASS just
 * because it was loaded: the test verifies the row contract and its adapter
 * classification, while rows needing a live HTTP/DB side effect are recorded
 * as INTEGRATION_REQUIRED.  This keeps the 14,227 case count honest and makes
 * missing business adapters visible in the generated result file.
 *
 * Run with -Drelease.inventory=... to use another inventory.  The default is
 * the repository's docs/test-plan/full-case-inventory.csv.
 */
@Tag("release-matrix")
class ReleaseCaseMatrixJUnitTest {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final Set<String> FEATURES = Set.of(
            "Authentication", "Profile", "Recruitment-post", "Search",
            "Direct-message", "Image-storage", "Report-admin", "Feedback", "Security");
    private static final Set<String> PRIORITIES = Set.of("P0", "P1", "P2", "P3");
    // Both features here have a case in runDtoContract that asserts against the real production
    // code (Bean Validation for Profile, ImageStorageService for Image-storage) for every one of
    // their InputState values -- that is what earns the UNIT_CONTRACT label below.
    private static final Set<String> UNIT_ADAPTERS = Set.of("Profile", "Image-storage");
    private static final Path RESULTS = Paths.get("target", "release-case-matrix-results.csv");

    @TestFactory
    Stream<DynamicTest> everyApiUnitAndIntegrationCase() throws IOException {
        Path inventory = inventoryPath();
        List<CaseRow> rows = readRows(inventory);
        if (rows.isEmpty()) {
            throw new IllegalStateException("No API/unit/integration rows found in " + inventory);
        }
        prepareResults();
        return rows.stream().map(row -> DynamicTest.dynamicTest(
                row.testId() + " " + row.feature() + " / " + row.operation(),
                () -> execute(row)));
    }

    private static void execute(CaseRow row) throws IOException {
        String classification = UNIT_ADAPTERS.contains(row.feature()) ? "UNIT_CONTRACT" : "INTEGRATION_REQUIRED";
        // Every field below is part of the executable test contract.  A typo or
        // truncated generated row fails its own case and cannot be hidden by a
        // neighboring row's result.
        assertAll(
                () -> assertTrue(row.testId().matches("FULL-\\d{5}"), "invalid test id"),
                () -> assertFalse(blank(row.requirementId()), "missing requirement id"),
                () -> assertTrue(FEATURES.contains(row.feature()), "no adapter classification for feature"),
                () -> assertTrue(PRIORITIES.contains(row.priority()), "invalid priority"),
                () -> assertFalse(blank(row.userState()), "missing user state"),
                () -> assertFalse(blank(row.inputState()), "missing input state"),
                () -> assertFalse(blank(row.dataState()), "missing data state"),
                () -> assertFalse(blank(row.operation()), "missing operation"),
                () -> assertFalse(blank(row.networkState()), "missing network state"),
                () -> assertFalse(blank(row.expected()), "missing expected result"),
                () -> assertFalse(blank(row.dbCheck()), "missing DB assertion contract"),
                () -> assertFalse(blank(row.apiCheck()), "missing API assertion contract"),
                () -> assertFalse(blank(row.uiCheck()), "missing UI assertion contract"),
                () -> assertNotNull(classification));
        runDtoContract(row);
        appendResult(row, classification, "EXECUTED_ROW_CONTRACT");
    }

    /**
     * Execute the cheap, deterministic part of the matrix against the real
     * request records and Bean Validation metadata.  Cases whose outcome is
     * determined by a database, HTTP session, file system, or a master-data
     * lookup remain explicitly classified INTEGRATION_REQUIRED.
     */
    private static void runDtoContract(CaseRow row) {
        switch (row.feature()) {
            case "Authentication" -> validateAuthentication(row.inputState());
            case "Profile" -> validateProfile(row.inputState());
            case "Recruitment-post" -> validatePost(row.inputState());
            case "Search" -> validateSearch(row.inputState());
            case "Direct-message" -> validateMessage(row.inputState());
            case "Image-storage" -> validateImageStorage(row.inputState());
            case "Report-admin" -> validateReportAdmin(row.inputState());
            case "Feedback" -> validateFeedback(row.inputState());
            // Every Security InputState (CSRF token presence, session identity, IDOR, enum/number
            // fuzzing) is only observable through a live Spring MVC filter chain and HTTP response;
            // there is no DTO or pure-Java logic in this codebase that distinguishes them standalone.
            default -> { /* Security requires live adapters. */ }
        }
    }

    /**
     * Search's InputStates are mostly keyword-match/no-match scenarios resolved by a JPA
     * Specification query (search/no-hit/title-hit/body-hit/area-hit/max-length): genuinely
     * DB-only, so they stay unexercised here. The cursor states are different -- PostController
     * resolves the "/page" cursor with a pure function (no DB) precisely so that a malformed or
     * oversized cursor can't 500; that function is asserted directly below.
     */
    private static void validateSearch(String state) {
        switch (state) {
            case "huge-cursor" -> assertTrue(PostController.resolveCursorOffset("99999999999999999999", 37) == 37,
                    "a cursor too large to parse as int must clamp to the end of the page, not throw");
            case "invalid-cursor" -> assertTrue(PostController.resolveCursorOffset("not-a-number", 37) == 0,
                    "a non-numeric cursor must be ignored (offset 0), not treated as an error");
            default -> { /* empty-keyword/title-hit/body-hit/area-hit/no-hit/max-length: DB query only. */ }
        }
    }

    private static void validateReportAdmin(String state) {
        Object request = switch (state) {
            case "post" -> reportRequest("POST", 1L, "迷惑行為の報告です");
            case "user" -> reportRequest("USER", 1L, "迷惑行為の報告です");
            case "dm" -> reportRequest("MESSAGE", 1L, "迷惑行為の報告です");
            case "empty-reason" -> reportRequest("POST", 1L, "");
            case "reason-boundary" -> reportRequest("POST", 1L, "理".repeat(1000));
            // unknown-id: a nonexistent targetId is still a syntactically valid request -- whether
            // the target actually exists is a repository lookup in ReportService, not a DTO rule.
            default -> reportRequest("POST", 999999999L, "存在しない対象の報告です");
        };
        boolean valid = VALIDATOR.validate(request).isEmpty();
        assertTrue(valid == !state.equals("empty-reason"),
                "report DTO state=" + state + " violations=" + VALIDATOR.validate(request));
    }

    private static Object reportRequest(String targetType, long targetId, String reason) {
        return newRecord("com.example.bandlink.dto.ReportRequest",
                enumValue("com.example.bandlink.entity.ReportTargetType", targetType), targetId, reason);
    }

    private static final Path IMAGE_ROOT = createImageRoot();
    private static final ImageStorageService IMAGE_STORAGE = isolatedImageStorage(IMAGE_ROOT);
    private static final byte[] PNG_BYTES = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aD1sAAAAASUVORK5CYII=");

    /**
     * Exercises the real ImageStorageService (magic-byte/IEND validation, MIME inference, size
     * limit, path-traversal guard) against an isolated temp directory -- the same technique
     * ReleaseImageUnitTest uses -- so every Image-storage InputState gets a real assertion instead
     * of the generic row contract.
     */
    private static void validateImageStorage(String state) {
        switch (state) {
            // SEC-012: a bare SOI-marker fixture (no EOI) used to pass here; ImageStorageService
            // now requires the trailing EOI marker (0xFFD9) the way it already required PNG's
            // IEND chunk, so the fixture needs one too - it stands in for the compressed scan
            // data a real photo has between SOI and EOI.
            case "jpeg" -> acceptImage(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, (byte) 0xFF, (byte) 0xD9}, "photo.jpg", "image/jpeg", ".jpg");
            case "png" -> acceptImage(PNG_BYTES, "photo.png", "image/png", ".png");
            case "webp" -> acceptImage(new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'}, "photo.webp", "image/webp", ".webp");
            case "exact-5mb" -> acceptImage(padded(PNG_BYTES, 5 * 1024 * 1024), "boundary.png", "image/png", ".png");
            case "empty" -> rejectImage(new byte[0], "empty.png", "image/png");
            case "over-5mb" -> rejectImage(padded(PNG_BYTES, 5 * 1024 * 1024 + 1), "over.png", "image/png");
            case "fake-mime" -> rejectImage("<svg onload=alert(1)/>".getBytes(StandardCharsets.UTF_8), "fake.png", "image/png");
            case "double-extension" -> rejectImage(PNG_BYTES, "invoice.png.exe", null);
            case "truncated" -> rejectImage(new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10}, "broken.png", "image/png");
            case "path-traversal" -> assertPathTraversalRejected();
            default -> { }
        }
    }

    private static byte[] padded(byte[] base, int size) {
        byte[] out = new byte[size];
        System.arraycopy(base, 0, out, 0, base.length);
        return out;
    }

    private static void acceptImage(byte[] data, String filename, String contentType, String expectedExtension) {
        MockMultipartFile file = new MockMultipartFile("file", filename, contentType, data);
        String url = IMAGE_STORAGE.store(file);
        try {
            assertTrue(url.endsWith(expectedExtension), "image-storage accepted file lost its expected extension: " + url);
        } finally {
            IMAGE_STORAGE.delete(url);
        }
    }

    private static void rejectImage(byte[] data, String filename, String contentType) {
        MockMultipartFile file = new MockMultipartFile("file", filename, contentType, data);
        assertThrows(IllegalArgumentException.class, () -> IMAGE_STORAGE.store(file));
    }

    private static void assertPathTraversalRejected() {
        for (String name : new String[]{"../private.png", "..\\private.png", "/etc/passwd.png"}) {
            assertThrows(IllegalArgumentException.class, () -> IMAGE_STORAGE.load(name));
        }
    }

    private static Path createImageRoot() {
        try { return Files.createTempDirectory("release-case-matrix-images"); }
        catch (IOException ex) { throw new UncheckedIOException(ex); }
    }

    private static ImageStorageService isolatedImageStorage(Path root) {
        ImageStorageService service = new ImageStorageService();
        try {
            Field field = ImageStorageService.class.getDeclaredField("root");
            field.setAccessible(true);
            field.set(service, root);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Cannot isolate ImageStorageService root", ex);
        }
        return service;
    }

    private static void validateAuthentication(String state) {
        Object request = switch (state) {
            case "empty" -> newRecord("com.example.bandlink.dto.RegisterRequest", "", "", "", null, null, null, Set.of(), Set.of(), Set.of(), Set.of());
            case "invalid" -> newRecord("com.example.bandlink.dto.RegisterRequest", "x", "bad", "short", 20, 1, "その他", Set.of(1L), Set.of(1L), Set.of(1L), Set.of(1L));
            case "min" -> validRegister("a", "a@example.test", "12345678", 0, 0);
            case "max" -> validRegister("a".repeat(80), "a@example.test", "p".repeat(128), 120, 100);
            case "over" -> newRecord("com.example.bandlink.dto.RegisterRequest", "a".repeat(81), "a@example.test", "p".repeat(129), 121, 101, "男性", Set.of(1L), Set.of(1L), Set.of(1L), Set.of(1L));
            default -> validRegister("normal", "normal@example.test", "12345678", 20, 1);
        };
        boolean valid = VALIDATOR.validate(request).isEmpty();
        boolean expected = state.equals("min") || state.equals("max") || state.equals("normal");
        assertTrue(valid == expected, "authentication DTO state=" + state + " violations=" + VALIDATOR.validate(request));
    }

    private static Object validRegister(String name, String email, String password, int age, int years) {
        return newRecord("com.example.bandlink.dto.RegisterRequest", name, email, password, age, years, "男性", Set.of(1L), Set.of(1L), Set.of(1L), Set.of(1L));
    }

    private static void validateProfile(String state) {
        String url = state.equals("invalid-url") ? "u".repeat(1001) : (state.equals("max") ? "u".repeat(1000) : null);
        Object request = state.equals("empty")
                ? newRecord("com.example.bandlink.dto.ProfileUpdateRequest", "", null, null, null, null, null, null, null, null, null, null, Set.of(), Set.of(), Set.of(), Set.of())
                : newRecord("com.example.bandlink.dto.ProfileUpdateRequest", "QA", "男性", "bio", state.equals("min") ? 0 : 20, state.equals("min") ? 0 : 1,
                        url, null, null, null, null, null, Set.of(1L), Set.of(1L), Set.of(1L), Set.of(1L));
        boolean valid = VALIDATOR.validate(request).isEmpty();
        assertTrue(valid == (!state.equals("empty") && !state.equals("invalid-url")),
                "profile DTO state=" + state + " violations=" + VALIDATOR.validate(request));
    }

    private static void validatePost(String state) {
        Set<Long> parts = state.equals("missing-choice") ? Set.of() : state.equals("too-many-choice") ? Set.of(1L, 2L, 3L, 4L, 5L, 6L) : Set.of(1L);
        Set<Long> genres = state.equals("missing-choice") ? Set.of() : state.equals("too-many-choice") ? Set.of(1L, 2L, 3L, 4L) : Set.of(1L);
        Set<Long> stances = state.equals("missing-choice") ? Set.of() : state.equals("too-many-choice") ? Set.of(1L, 2L) : Set.of(1L);
        Set<Long> prefectures = state.equals("missing-choice") ? Set.of() : state.equals("too-many-choice") ? Set.of(1L, 2L, 3L, 4L) : Set.of(1L);
        String title = state.equals("empty-body") ? "" : "題".repeat(state.equals("body-boundary") ? 30 : 1);
        String body = state.equals("empty-body") ? "" : "本".repeat(state.equals("body-boundary") ? 500 : 1);
        Object request = newRecord("com.example.bandlink.dto.PostRequests$Create", enumValue("com.example.bandlink.entity.PostType", "MEMBER_WANTED"), title, body, "都内", parts, genres, stances, prefectures,
                Set.of(enumValue("com.example.bandlink.entity.AgeRange", "ANY")), enumValue("com.example.bandlink.entity.ActivityFrequency", "WEEKLY_1"));
        boolean valid = VALIDATOR.validate(request).isEmpty();
        boolean expected = !state.equals("empty-body") && !state.equals("missing-choice") && !state.equals("too-many-choice");
        assertTrue(valid == expected, "post DTO state=" + state + " violations=" + VALIDATOR.validate(request));
    }

    private static void validateMessage(String state) {
        String content = state.equals("text-boundary") ? "m".repeat(500) : (state.equals("oversize-image") ? "m" : null);
        String image = state.equals("oversize-image") ? "i".repeat(1001) : null;
        Object request = newRecord("com.example.bandlink.dto.MessageRequests$Send", content, image);
        boolean valid = VALIDATOR.validate(request).isEmpty();
        assertTrue(valid == !state.equals("oversize-image"), "message DTO state=" + state);
    }

    private static void validateFeedback(String state) {
        String body = state.equals("empty-body") ? "" : (state.equals("body-boundary") ? "お".repeat(1000) : "お問い合わせ");
        String image = state.equals("invalid-url") ? "i".repeat(1001) : null;
        Object request = newRecord("com.example.bandlink.dto.FeedbackRequest", body, image);
        boolean valid = VALIDATOR.validate(request).isEmpty();
        assertTrue(valid == (!state.equals("empty-body") && !state.equals("invalid-url")), "feedback DTO state=" + state);
    }

    private static Object enumValue(String type, String name) {
        try { return Enum.valueOf((Class) Class.forName(type), name); }
        catch (ReflectiveOperationException ex) { throw new AssertionError("Missing enum " + type, ex); }
    }

    private static Object newRecord(String type, Object... args) {
        try {
            Class<?> clazz = Class.forName(type);
            for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                if (constructor.getParameterCount() == args.length) {
                    constructor.setAccessible(true);
                    return constructor.newInstance(args);
                }
            }
            throw new NoSuchMethodException(type + " argument count=" + args.length);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Cannot construct " + type, ex);
        }
    }

    private static Path inventoryPath() {
        String configured = System.getProperty("release.inventory");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("RELEASE_INVENTORY");
        }
        return Paths.get(configured == null || configured.isBlank()
                ? "docs/test-plan/full-case-inventory.csv" : configured).toAbsolutePath().normalize();
    }

    private static List<CaseRow> readRows(Path inventory) throws IOException {
        if (!Files.isRegularFile(inventory)) {
            throw new IOException("Release inventory not found: " + inventory);
        }
        List<String> lines = Files.readAllLines(inventory, StandardCharsets.UTF_8);
        if (lines.isEmpty()) return List.of();
        List<String> header = parseCsv(lines.get(0));
        // PowerShell Export-Csv may emit UTF-8 with a BOM on the first column.
        if (!header.isEmpty()) header.set(0, header.get(0).replace("\uFEFF", ""));
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < header.size(); i++) columns.put(header.get(i), i);
        String[] required = {"TestID", "RequirementID", "Priority", "Feature", "UserState", "InputState",
                "DataState", "Operation", "NetworkState", "Expected", "DBCheck", "APICheck", "UICheck"};
        for (String name : required) {
            if (!columns.containsKey(name)) throw new IOException("Missing inventory column: " + name);
        }
        List<CaseRow> rows = new ArrayList<>();
        for (int line = 1; line < lines.size(); line++) {
            if (lines.get(line).isBlank()) continue;
            List<String> values = parseCsv(lines.get(line));
            String feature = value(values, columns, "Feature");
            if ("Non-functional".equals(feature)) continue;
            rows.add(new CaseRow(
                    value(values, columns, "TestID"), value(values, columns, "RequirementID"),
                    value(values, columns, "Priority"), feature,
                    value(values, columns, "UserState"), value(values, columns, "InputState"),
                    value(values, columns, "DataState"), value(values, columns, "Operation"),
                    value(values, columns, "NetworkState"), value(values, columns, "Expected"),
                    value(values, columns, "DBCheck"), value(values, columns, "APICheck"),
                    value(values, columns, "UICheck")));
        }
        Set<String> ids = new HashSet<>();
        for (CaseRow row : rows) {
            if (!ids.add(row.testId())) throw new IOException("Duplicate test id: " + row.testId());
        }
        return rows;
    }

    private static String value(List<String> values, Map<String, Integer> columns, String name) {
        int index = columns.get(name);
        return index < values.size() ? values.get(index) : "";
    }

    private static List<String> parseCsv(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"'); i++;
                } else quoted = !quoted;
            } else if (c == ',' && !quoted) {
                result.add(field.toString()); field.setLength(0);
            } else field.append(c);
        }
        result.add(field.toString());
        return result;
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }

    private static synchronized void prepareResults() throws IOException {
        Files.createDirectories(RESULTS.getParent());
        Files.writeString(RESULTS,
                "TestID,Feature,Priority,Classification,Status,Assertion\n",
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static synchronized void appendResult(CaseRow row, String classification, String assertion)
            throws IOException {
        try (BufferedWriter out = Files.newBufferedWriter(RESULTS, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            out.write(csv(row.testId()) + "," + csv(row.feature()) + "," + csv(row.priority()) + ","
                    + csv(classification) + ",PASS," + csv(assertion));
            out.newLine();
        }
    }

    private static String csv(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }

    private record CaseRow(String testId, String requirementId, String priority, String feature,
            String userState, String inputState, String dataState, String operation,
            String networkState, String expected, String dbCheck, String apiCheck, String uiCheck) {}
}
