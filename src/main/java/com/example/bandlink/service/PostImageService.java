package com.example.bandlink.service;

import com.example.bandlink.entity.Post;
import com.example.bandlink.entity.PostImage;
import com.example.bandlink.entity.PostStatus;
import com.example.bandlink.entity.User;
import com.example.bandlink.repository.PostImageRepository;
import com.example.bandlink.repository.PostRepository;
import com.example.bandlink.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PostImageService {
    private final PostRepository posts;
    private final PostImageRepository images;
    private final UserRepository users;
    private final ImageStorageService storage;
    private final Clock clock = Clock.systemDefaultZone();

    public PostImageService(PostRepository posts, PostImageRepository images, UserRepository users, ImageStorageService storage) {
        this.posts = posts; this.images = images; this.users = users; this.storage = storage;
    }
    @Transactional public PostImage add(Long userId, Long postId, MultipartFile file) {
        Post post = posts.findByIdAndUserId(postId,userId).orElseThrow(() -> new PostService.RuleViolationException("投稿が見つかりません"));
        User user = owner(postId, userId); editAllowed(user, post);
        if (images.countByPostId(postId) >= 5) throw new PostService.RuleViolationException("募集画像は5枚までです");
        String url = storage.store(file);
        try {
            PostImage image = images.saveAndFlush(new PostImage(post, url, (int)images.countByPostId(postId), now()));
            user.setLastEditedAt(now());
            return image;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // NFT-004: the countByPostId check above is not atomic with this insert - a concurrent
            // upload to the same post can pass it too, and lose the race to trg_post_images_limit
            // (DatabaseConstraintInitializer). Delete the file already written to disk so the
            // rejected DB row does not leave an orphan upload behind, and give the same user-facing
            // message the synchronous over-5 case gets, not a raw 500.
            storage.delete(url);
            throw new PostService.RuleViolationException("募集画像は5枚までです");
        }
    }
    @Transactional public List<PostImage> addAll(Long userId, Long postId, List<MultipartFile> files) {
        Post post = posts.findByIdAndUserId(postId,userId).orElseThrow(() -> new PostService.RuleViolationException("投稿が見つかりません"));
        User user = owner(postId, userId); editAllowed(user, post);
        if (files == null || files.isEmpty() || files.size() + images.countByPostId(postId) > 5) throw new PostService.RuleViolationException("募集画像は5枚までです");
        int order = (int)images.countByPostId(postId); List<PostImage> result = new java.util.ArrayList<>();
        List<String> storedUrls = new java.util.ArrayList<>();
        try {
            for (MultipartFile file : files) {
                String url = storage.store(file);
                storedUrls.add(url);
                result.add(images.saveAndFlush(new PostImage(post, url, order++, now())));
            }
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Same race as add(): this whole @Transactional method rolls back on the exception,
            // which undoes every PostImage row already flushed in this loop too, so every file this
            // request wrote to disk (including ones already "saved") becomes an orphan unless
            // deleted here.
            for (String url : storedUrls) storage.delete(url);
            throw new PostService.RuleViolationException("募集画像は5枚までです");
        }
        user.setLastEditedAt(now()); return result;
    }
    @Transactional public void remove(Long userId, Long postId, Long imageId) {
        Post post = posts.findByIdAndUserId(postId,userId).orElseThrow(() -> new PostService.RuleViolationException("投稿が見つかりません"));
        User user = owner(postId, userId); editAllowed(user, post);
        PostImage image = images.findById(imageId).orElseThrow(() -> new PostService.RuleViolationException("画像が見つかりません"));
        if (!postId.equals(image.getPostId())) throw new PostService.RuleViolationException("画像が見つかりません");
        storage.delete(image.getImageUrl()); images.delete(image); user.setLastEditedAt(now());
    }
    @Transactional public void reorder(Long userId, Long postId, List<Long> orderedIds) {
        Post post = posts.findByIdAndUserId(postId,userId).orElseThrow(() -> new PostService.RuleViolationException("投稿が見つかりません"));
        User user = owner(postId, userId); editAllowed(user, post);
        List<PostImage> current = images.findByPostIdOrderBySortOrderAsc(postId);
        if (orderedIds == null || orderedIds.size() != current.size() || !orderedIds.containsAll(current.stream().map(PostImage::getId).toList()))
            throw new PostService.RuleViolationException("画像の並び順を確認してください");
        for (int i=0;i<orderedIds.size();i++) {
            Long imageId = orderedIds.get(i);
            for (PostImage image : current) if (image.getId().equals(imageId)) image.setSortOrder(i);
        }
        user.setLastEditedAt(now());
    }
    @Transactional(readOnly=true) public List<PostImage> list(Long postId) {
        Post post = posts.findById(postId).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "投稿が見つかりません"));
        if (!post.getUser().isActive() || (post.getStatus() == PostStatus.CLOSED
                && post.getClosedReason() != com.example.bandlink.entity.ClosedReason.MANUAL
                && post.getClosedReason() != com.example.bandlink.entity.ClosedReason.EXPIRED))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "投稿が見つかりません");
        return images.findByPostIdOrderBySortOrderAsc(postId);
    }
    private User owner(Long postId, Long userId) { Post p=posts.findByIdAndUserId(postId,userId).orElseThrow(()->new PostService.RuleViolationException("投稿が見つかりません")); if(p.getStatus()!=PostStatus.OPEN)throw new PostService.RuleViolationException("公開中の投稿のみ画像を変更できます"); return users.findById(userId).orElseThrow(); }
    private static final DateTimeFormatter EDIT_LOCK_UNTIL_FORMAT = DateTimeFormatter.ofPattern("M月d日 H:mm", Locale.JAPAN);
    private void editAllowed(User u, Post p){
        if (u.getLastEditedAt() == null || sameWriteWindow(u, p)) return;
        LocalDateTime availableAt = u.getLastEditedAt().plusHours(12);
        if (availableAt.isAfter(now()))
            throw new PostService.RuleViolationException("投稿の編集は12時間に1回までです。次に編集できるのは"
                    + availableAt.format(EDIT_LOCK_UNTIL_FORMAT) + "以降です。");
    }
    private boolean sameWriteWindow(User u, Post p) {
        LocalDateTime reference = p.getUpdatedAt() == null ? p.getCreatedAt() : p.getUpdatedAt();
        return reference != null && Duration.between(reference, u.getLastEditedAt()).abs().compareTo(Duration.ofSeconds(2)) <= 0;
    }
    private LocalDateTime now(){return LocalDateTime.now(clock);}
}
