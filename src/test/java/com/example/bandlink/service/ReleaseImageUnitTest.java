package com.example.bandlink.service;

import java.nio.file.*;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.io.CleanupMode;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

class ReleaseImageUnitTest {
    // Windows can retain a handle to the temporary directory until the JVM exits; avoid masking
    // assertion results with JUnit's recursive cleanup failure. The disposable runner removes it.
    @TempDir(cleanup = CleanupMode.NEVER) Path directory;
    final ImageStorageService storage = new ImageStorageService();
    static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aD1sAAAAASUVORK5CYII=");

    @BeforeEach void isolateStorage() { ReflectionTestUtils.setField(storage, "root", directory.toAbsolutePath().normalize()); }

    @Test void codeUt017_pngRoundTripAndDeleteStayInsideTempDirectory() throws Exception {
        String url = storage.store(new MockMultipartFile("file", "../../outside.png", "image/png", PNG));
        String name = url.substring(url.lastIndexOf('/') + 1);
        assertTrue(name.matches("[a-f0-9-]{36}\\.png"));
        assertTrue(storage.load(name).getFile().toPath().normalize().startsWith(directory.toAbsolutePath().normalize()));
        assertArrayEquals(PNG, storage.load(name).getContentAsByteArray());
        storage.delete(url);
        assertFalse(Files.exists(directory.resolve(name)));
    }
    @Test void codeUt018_missingOrGenericMimeUsesExtensionAndValidatesSignature() {
        for (String mime : new String[]{null, "", "application/octet-stream"}) {
            assertTrue(storage.store(new MockMultipartFile("file", "PHONE.PNG", mime, PNG)).endsWith(".png"));
        }
        assertThrows(IllegalArgumentException.class,
                () -> storage.store(new MockMultipartFile("file", "phone.jpg", "application/octet-stream", PNG)));
    }
    @Test void codeUt019_emptyOversizeAndUnsupportedFilesAreRejectedWithoutWrites() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> storage.store(null));
        assertThrows(IllegalArgumentException.class,
                () -> storage.store(new MockMultipartFile("file", "a.png", "image/png", new byte[0])));
        byte[] oversize = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, oversize, 0, PNG.length);
        assertThrows(IllegalArgumentException.class,
                () -> storage.store(new MockMultipartFile("file", "a.png", "image/png", oversize)));
        assertThrows(IllegalArgumentException.class,
                () -> storage.store(new MockMultipartFile("file", "a.svg", "image/svg+xml", "<svg/>".getBytes())));
        try (var files = Files.list(directory)) { assertEquals(0, files.count()); }
    }
    @Test void codeUt020_fiveMiBFileIsAcceptedAtExactByteBoundary() {
        byte[] limit = new byte[5 * 1024 * 1024];
        System.arraycopy(PNG, 0, limit, 0, PNG.length);
        assertTrue(storage.store(new MockMultipartFile("file", "boundary.png", "image/png", limit)).endsWith(".png"));
    }
    @Test void codeUt021_pathTraversalCannotReadFileOutsideStorage() {
        for (String name : new String[]{"../private.png", "..\\private.png", "/tmp/a.png", "a.png?x=1"}) {
            assertThrows(IllegalArgumentException.class, () -> storage.load(name));
        }
    }
    @Test void codeUt022_truncatedImageMustBeRejectedEvenWithCorrectMagicBytes() {
        // Acceptance target: a file that cannot be decoded must not be accepted as an image.
        // Static inspection found signature-only validation; keep the assertion, do not weaken it.
        byte[] truncated = new byte[]{(byte)137, 80, 78, 71, 13, 10, 26, 10};
        assertThrows(IllegalArgumentException.class,
                () -> storage.store(new MockMultipartFile("file", "broken.png", "image/png", truncated)));
    }
    /**
     * PW-E follow-up (ST-038/ST-017): found via Playwright MCP with a real (non-degenerate) WebP
     * upload. The signature check required the file to be exactly the 12-byte bare RIFF/WEBP header
     * with no image payload after it - true only of a placeholder that carries no actual pixels. Any
     * real-world WebP always has a VP8/VP8L/VP8X chunk following that header, so every real WebP
     * upload was being rejected as "画像形式を確認できません" (500 at the HTTP layer, see
     * ReleaseApiIntegrationTest.it033, because the error path could not be content-negotiated either).
     */
    @Test void codeUt023_realisticallySizedWebpWithPayloadAfterTheHeaderIsAccepted() {
        byte[] header = {'R','I','F','F', 0,0,0,0, 'W','E','B','P'};
        byte[] webp = new byte[header.length + 40];
        System.arraycopy(header, 0, webp, 0, header.length);
        // The bytes after the header stand in for a VP8 chunk; their content doesn't matter to the
        // signature check, only that a real file never stops at byte 12 the way the fixture used to.
        assertTrue(storage.store(new MockMultipartFile("file", "photo.webp", "image/webp", webp)).endsWith(".webp"));
    }
    /**
     * SEC-012 (PW-H): found via Playwright MCP - a multipart upload consisting only of the JPEG
     * SOI/APPn marker (0xFFD8FF) followed by arbitrary garbage was accepted, written to disk, and
     * served back at a real URL, because the JPEG branch of valid() checked only those first 3
     * bytes. PNG already required its IEND chunk for exactly this reason; JPEG now requires its
     * own closing marker, the EOI (0xFFD9), the same way.
     */
    @Test void codeUt024_jpegWithoutAnEoiMarkerIsRejectedEvenWithACorrectSoiHeader() {
        byte[] garbage = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        assertThrows(IllegalArgumentException.class,
                () -> storage.store(new MockMultipartFile("file", "fake.jpg", "image/jpeg", garbage)));
    }
    @Test void codeUt025_realisticJpegWithScanDataAndEoiMarkerIsAccepted() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0, 1, 2, 3, 4, 5, (byte) 0xFF, (byte) 0xD9};
        assertTrue(storage.store(new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpeg)).endsWith(".jpg"));
    }
}
