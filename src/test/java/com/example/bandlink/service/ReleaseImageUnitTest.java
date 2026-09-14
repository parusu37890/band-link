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
}
