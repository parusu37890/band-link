package com.example.bandlink.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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
        if (file == null || file.isEmpty() || file.getSize() > MAX || !TYPES.contains(file.getContentType()))
            throw new IllegalArgumentException("画像はjpg/png/webp、1枚5MBまでです");
        try {
            byte[] data = file.getBytes();
            if (!valid(file.getContentType(), Arrays.copyOf(data, Math.min(12, data.length))))
                throw new IllegalArgumentException("画像形式を確認できません");
            Files.createDirectories(root);
            String ext = switch (file.getContentType()) { case "image/jpeg" -> ".jpg"; case "image/png" -> ".png"; default -> ".webp"; };
            String name = UUID.randomUUID() + ext;
            Path target = root.resolve(name).normalize();
            if (!target.startsWith(root)) throw new IllegalArgumentException("不正なファイル名です");
            Files.write(target, data);
            return "/uploads/" + name;
        } catch (IOException e) { throw new IllegalStateException("画像保存に失敗しました", e); }
    }
    public Resource load(String name) {
        if (name == null || !name.matches("[A-Za-z0-9-]+\\.(jpg|png|webp)")) throw new IllegalArgumentException("不正な画像名です");
        Path target = root.resolve(name).normalize();
        if (!target.startsWith(root) || !Files.isRegularFile(target)) throw new IllegalArgumentException("画像が見つかりません");
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
        if (type.equals("image/png")) return b.length >= 8 && (b[0]&255)==137 && b[1]==80 && b[2]==78 && b[3]==71 && b[4]==13 && b[5]==10 && b[6]==26 && b[7]==10;
        return b.length == 12 && b[0]==82 && b[1]==73 && b[2]==70 && b[3]==70 && b[8]==87 && b[9]==69 && b[10]==66 && b[11]==80;
    }
}
