package com.example.bandlink.service;

import com.example.bandlink.entity.Post;
import com.example.bandlink.entity.PostImage;
import com.example.bandlink.entity.PostStatus;
import com.example.bandlink.repository.PostImageRepository;
import com.example.bandlink.repository.PostRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PostImageService {
    // Kept in sync by hand with enforce_post_images_limit()'s own "current_count >= 4"
    // (DatabaseConstraintInitializer) - the trigger has no way to read a Java constant.
    private static final int MAX_IMAGES = 4;
    private final PostRepository posts;
    private final PostImageRepository images;
    private final ImageStorageService storage;
    private final Clock clock = Clock.systemDefaultZone();

    public PostImageService(PostRepository posts, PostImageRepository images, ImageStorageService storage) {
        this.posts = posts; this.images = images; this.storage = storage;
    }
    @Transactional public PostImage add(Long userId, Long postId, MultipartFile file) {
        Post post = openOwnedPost(userId, postId);
        if (images.countByPostId(postId) >= MAX_IMAGES) throw new PostService.RuleViolationException("募集画像は" + MAX_IMAGES + "枚までです");
        String url = storage.store(file);
        try {
            return images.saveAndFlush(new PostImage(post, url, (int)images.countByPostId(postId), now()));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // NFT-004: the countByPostId check above is not atomic with this insert - a concurrent
            // upload to the same post can pass it too, and lose the race to trg_post_images_limit
            // (DatabaseConstraintInitializer). Delete the file already written to disk so the
            // rejected DB row does not leave an orphan upload behind, and give the same user-facing
            // message the synchronous over-limit case gets, not a raw 500.
            storage.delete(url);
            throw new PostService.RuleViolationException("募集画像は" + MAX_IMAGES + "枚までです");
        }
    }
    @Transactional public List<PostImage> addAll(Long userId, Long postId, List<MultipartFile> files) {
        Post post = openOwnedPost(userId, postId);
        if (files == null || files.isEmpty() || files.size() + images.countByPostId(postId) > MAX_IMAGES) throw new PostService.RuleViolationException("募集画像は" + MAX_IMAGES + "枚までです");
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
            throw new PostService.RuleViolationException("募集画像は" + MAX_IMAGES + "枚までです");
        }
        return result;
    }
    @Transactional public void remove(Long userId, Long postId, Long imageId) {
        openOwnedPost(userId, postId);
        PostImage image = images.findById(imageId).orElseThrow(() -> new PostService.RuleViolationException("画像が見つかりません"));
        if (!postId.equals(image.getPostId())) throw new PostService.RuleViolationException("画像が見つかりません");
        storage.delete(image.getImageUrl()); images.delete(image);
    }
    @Transactional public void reorder(Long userId, Long postId, List<Long> orderedIds) {
        openOwnedPost(userId, postId);
        List<PostImage> current = images.findByPostIdOrderBySortOrderAsc(postId);
        if (orderedIds == null || orderedIds.size() != current.size() || !orderedIds.containsAll(current.stream().map(PostImage::getId).toList()))
            throw new PostService.RuleViolationException("画像の並び順を確認してください");
        for (int i=0;i<orderedIds.size();i++) {
            Long imageId = orderedIds.get(i);
            for (PostImage image : current) if (image.getId().equals(imageId)) image.setSortOrder(i);
        }
    }
    @Transactional(readOnly=true) public List<PostImage> list(Long postId) {
        Post post = posts.findById(postId).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "投稿が見つかりません"));
        if (!post.getUser().isActive() || (post.getStatus() == PostStatus.CLOSED
                && post.getClosedReason() != com.example.bandlink.entity.ClosedReason.MANUAL
                && post.getClosedReason() != com.example.bandlink.entity.ClosedReason.EXPIRED))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "投稿が見つかりません");
        return images.findByPostIdOrderBySortOrderAsc(postId);
    }
    private Post openOwnedPost(Long userId, Long postId) {
        Post post = posts.findByIdAndUserId(postId, userId).orElseThrow(() -> new PostService.RuleViolationException("投稿が見つかりません"));
        if (post.getStatus() != PostStatus.OPEN) throw new PostService.RuleViolationException("公開中の投稿のみ画像を変更できます");
        return post;
    }
    private LocalDateTime now(){return LocalDateTime.now(clock);}
}
