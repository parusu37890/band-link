package com.example.bandlink.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageStorageService {
    private static final long MAX = 5 * 1024 * 1024;
    private static final Set<String> TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private final Path root = Paths.get(System.getenv().getOrDefault("BANDLINK_UPLOAD_DIR", "uploads")).toAbsolutePath().normalize();

    public String store(MultipartFile file) {
        return storeAt(file, root, "/uploads/");
    }
    public String storePrivate(MultipartFile file) {
        return storeAt(file, root.resolve("messages"), "/api/messages/images/");
    }
    // SEC-011: feedback attachments (contact/feature-request screenshots, often showing account
    // details or error content) were going through store(), the same public /uploads/ tree as
    // profile and post images - anyone with the URL could view them, admin role or not. This puts
    // them under their own private directory served only by AdminFeedbackController's
    // hasRole("ADMIN")-gated endpoint, the same pattern storePrivate() already uses for DM images.
    public String storeFeedback(MultipartFile file) {
        return storeAt(file, root.resolve("feedback"), "/api/admin/feedback/images/");
    }
    private String storeAt(MultipartFile file, Path destination, String publicPrefix) {
        String type = normalizedType(file);
        if (file == null || file.isEmpty() || file.getSize() > MAX || !TYPES.contains(type))
            throw new IllegalArgumentException("画像はjpg/png/webp、1枚5MBまでです");
        try {
            byte[] data = file.getBytes();
            if (!valid(type, data))
                throw new IllegalArgumentException("画像形式を確認できません");
            Files.createDirectories(destination);
            String ext = switch (type) { case "image/jpeg" -> ".jpg"; case "image/png" -> ".png"; default -> ".webp"; };
            String name = UUID.randomUUID() + ext;
            Path target = destination.resolve(name).normalize();
            if (!target.startsWith(destination)) throw new IllegalArgumentException("不正なファイル名です");
            Files.write(target, data);
            return publicPrefix + name;
        } catch (IOException e) { throw new IllegalStateException("画像保存に失敗しました", e); }
    }
    public Resource load(String name) {
        return loadAt(name, root);
    }
    public Resource loadPrivate(String name) {
        try { return loadAt(name, root.resolve("messages")); }
        catch (IllegalArgumentException ex) { return loadAt(name, root); }
    }
    public Resource loadFeedback(String name) {
        return loadAt(name, root.resolve("feedback"));
    }
    private Resource loadAt(String name, Path base) {
        if (name == null || !name.matches("[A-Za-z0-9-]+\\.(jpg|png|webp)")) throw new IllegalArgumentException("不正な画像名です");
        Path target = base.resolve(name).normalize();
        if (!target.startsWith(base) || !Files.isRegularFile(target)) throw new IllegalArgumentException("画像が見つかりません");
        return new FileSystemResource(target);
    }
    public void delete(String url) {
        if (url == null) return;
        String name = url.substring(url.lastIndexOf('/') + 1);
        if (!name.matches("[A-Za-z0-9-]+\\.(jpg|png|webp)")) return;
        try { Files.deleteIfExists(root.resolve(name).normalize()); }
        catch (IOException e) { throw new IllegalStateException("画像削除に失敗しました", e); }
    }
    private boolean valid(String type, byte[] b) {
        if (type.equals("image/jpeg")) return b.length >= 3 && (b[0]&255)==255 && (b[1]&255)==216 && (b[2]&255)==255;
        if (type.equals("image/png")) {
            if (b.length < 8 || (b[0]&255)!=137 || b[1]!=80 || b[2]!=78 || b[3]!=71 || b[4]!=13 || b[5]!=10 || b[6]!=26 || b[7]!=10) return false;
            // A signature-only or truncated PNG is not an image. Require the mandatory IEND chunk.
            for (int i = 8; i + 12 <= b.length; i++) {
                if (b[i]==0 && b[i+1]==0 && b[i+2]==0 && b[i+3]==0
                        && b[i+4]==73 && b[i+5]==69 && b[i+6]==78 && b[i+7]==68) return true;
            }
            return false;
        }
        // A real WebP file is never just the bare 12-byte RIFF/WEBP header: a VP8/VP8L/VP8X chunk
        // with the actual pixel data always follows it, so the file is always larger than 12 bytes.
        // Requiring an exact 12-byte length here rejected every real-world WebP image (only the
        // degenerate no-pixel-data fixture happened to be exactly that size).
        return b.length >= 12 && b[0]==82 && b[1]==73 && b[2]==70 && b[3]==70 && b[8]==87 && b[9]==69 && b[10]==66 && b[11]==80;
    }

    /** Browsers and native file pickers sometimes omit or mislabel the multipart MIME type. */
    private String normalizedType(MultipartFile file) {
        if (file == null) return "";
        String type = file.getContentType();
        if ("image/jpg".equalsIgnoreCase(type)) return "image/jpeg";
        if (type != null && !type.isBlank() && !"application/octet-stream".equalsIgnoreCase(type)) return type.toLowerCase(java.util.Locale.ROOT);
        String name = file.getOriginalFilename();
        if (name == null) return "";
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        return "";
    }

}
