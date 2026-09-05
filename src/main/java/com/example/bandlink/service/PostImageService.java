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
import java.util.List;
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
        PostImage image = images.save(new PostImage(post, url, (int)images.countByPostId(postId), now()));
        user.setLastEditedAt(now());
        return image;
    }
    @Transactional public List<PostImage> addAll(Long userId, Long postId, List<MultipartFile> files) {
        Post post = posts.findByIdAndUserId(postId,userId).orElseThrow(() -> new PostService.RuleViolationException("投稿が見つかりません"));
        User user = owner(postId, userId); editAllowed(user, post);
        if (files == null || files.isEmpty() || files.size() + images.countByPostId(postId) > 5) throw new PostService.RuleViolationException("募集画像は5枚までです");
        int order = (int)images.countByPostId(postId); List<PostImage> result = new java.util.ArrayList<>();
        for (MultipartFile file : files) result.add(images.save(new PostImage(post, storage.store(file), order++, now())));
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
    private void editAllowed(User u, Post p){
        if (u.getLastEditedAt() != null && u.getLastEditedAt().isAfter(now().minusHours(12)) && !sameWriteWindow(u, p))
            throw new PostService.RuleViolationException("投稿の作成・編集は12時間に1回までです");
    }
    private boolean sameWriteWindow(User u, Post p) {
        LocalDateTime reference = p.getUpdatedAt() == null ? p.getCreatedAt() : p.getUpdatedAt();
        return reference != null && Duration.between(reference, u.getLastEditedAt()).abs().compareTo(Duration.ofSeconds(2)) <= 0;
    }
    private LocalDateTime now(){return LocalDateTime.now(clock);}
}
