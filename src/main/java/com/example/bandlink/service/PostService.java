package com.example.bandlink.service;

import com.example.bandlink.dto.PostRequests;
import com.example.bandlink.entity.*;
import com.example.bandlink.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostService {
    private static final long EDIT_LOCK_HOURS = 12;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PartRepository partRepository;
    private final GenreRepository genreRepository;
    private final StanceRepository stanceRepository;
    private final PrefectureRepository prefectureRepository;
    private final Clock clock;

    public PostService(PostRepository posts, UserRepository users, PartRepository parts, GenreRepository genres,
                       StanceRepository stances, PrefectureRepository prefectures) {
        this(posts, users, parts, genres, stances, prefectures, Clock.systemDefaultZone());
    }
    PostService(PostRepository posts, UserRepository users, PartRepository parts, GenreRepository genres,
                StanceRepository stances, PrefectureRepository prefectures, Clock clock) {
        this.postRepository = posts; this.userRepository = users; this.partRepository = parts; this.genreRepository = genres;
        this.stanceRepository = stances; this.prefectureRepository = prefectures;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Transactional
    public Post create(Long userId, PostRequests.Create request) {
        User user = activeVerifiedUser(userId);
        checkEditLock(user);
        if (postRepository.existsByUserIdAndStatus(userId, PostStatus.OPEN)) throw new RuleViolationException("公開中の募集は1件までです");
        LocalDateTime now = now();
        Post post = new Post(user, request.type(), request.title().trim(), request.content().trim(), request.areaSub(), request.activityFrequency(), now);
        assign(post, request.partIds(), request.genreIds(), request.stanceIds(), request.prefectureIds(), request.ageRanges());
        user.setLastEditedAt(now);
        return postRepository.save(post);
    }

    @Transactional
    public Post update(Long userId, Long postId, PostRequests.Update request) {
        User user = activeVerifiedUser(userId); checkEditLock(user);
        Post post = owned(postId, userId);
        if (post.getStatus() != PostStatus.OPEN) throw new RuleViolationException("公開中の投稿のみ編集できます");
        post.update(request.title().trim(), request.content().trim(), request.areaSub(), request.activityFrequency(), now());
        assign(post, request.partIds(), request.genreIds(), request.stanceIds(), request.prefectureIds(), request.ageRanges());
        user.setLastEditedAt(now());
        return post;
    }

    @Transactional public void close(Long userId, Long postId) { activeUser(userId); Post post = owned(postId, userId); post.close(ClosedReason.MANUAL, now()); }

    @Transactional
    public Post reopen(Long userId, Long postId) {
        User user = activeVerifiedUser(userId); Post post = owned(postId, userId);
        if (post.getStatus() != PostStatus.CLOSED || (post.getClosedReason() != ClosedReason.MANUAL && post.getClosedReason() != ClosedReason.EXPIRED))
            throw new RuleViolationException("この投稿は再公開できません");
        if (postRepository.existsByUserIdAndStatus(userId, PostStatus.OPEN)) throw new RuleViolationException("公開中の募集は1件までです");
        LocalDateTime now = now(); post.reopen(now);
        if (user.getLastRankBoostedAt() == null || !user.getLastRankBoostedAt().isAfter(now.minusHours(EDIT_LOCK_HOURS))) { post.boostRank(now); user.setLastRankBoostedAt(now); }
        return post;
    }

    @Transactional
    public List<Post> listOpen() {
        LocalDateTime now = now();
        return postRepository.findByStatusOrderByRankUpdatedAtDesc(PostStatus.OPEN).stream()
                .peek(post -> expireIfNeeded(post, now)).filter(post -> post.getStatus() == PostStatus.OPEN).collect(Collectors.toList());
    }

    @Transactional
    public Post getPublic(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuleViolationException("投稿が見つかりません"));
        expireIfNeeded(post, now());
        return post;
    }

    private void expireIfNeeded(Post post, LocalDateTime now) {
        if (post.getStatus() == PostStatus.OPEN && !post.getExpiresAt().isAfter(now)) post.close(ClosedReason.EXPIRED, now);
    }

    private User activeVerifiedUser(Long id) { User user = activeUser(id); if (!user.isEmailVerified()) throw new RuleViolationException("メールアドレスの確認が必要です"); return user; }
    private User activeUser(Long id) { User user = userRepository.findById(id).orElseThrow(() -> new RuleViolationException("ユーザーが見つかりません")); if (user.getStatus() != UserStatus.ACTIVE) throw new RuleViolationException("現在この操作は利用できません"); return user; }
    private Post owned(Long postId, Long userId) { return postRepository.findByIdAndUserId(postId, userId).orElseThrow(() -> new RuleViolationException("投稿が見つかりません")); }
    private void checkEditLock(User user) { if (user.getLastEditedAt() != null && user.getLastEditedAt().isAfter(now().minusHours(EDIT_LOCK_HOURS))) throw new RuleViolationException("投稿の作成・編集は12時間に1回までです"); }
    private void assign(Post p, java.util.Set<Long> partIds, java.util.Set<Long> genreIds, java.util.Set<Long> stanceIds, java.util.Set<Long> prefectureIds, java.util.Set<AgeRange> ages) {
        p.getParts().clear(); p.getParts().addAll(partRepository.findAllById(partIds)); p.getGenres().clear(); p.getGenres().addAll(genreRepository.findAllById(genreIds));
        p.getStances().clear(); p.getStances().addAll(stanceRepository.findAllById(stanceIds)); p.getPrefectures().clear(); p.getPrefectures().addAll(prefectureRepository.findAllById(prefectureIds == null ? java.util.Set.of() : prefectureIds)); p.getAgeRanges().clear(); p.getAgeRanges().addAll(ages);
    }
    private LocalDateTime now() { return LocalDateTime.now(clock); }
    public static class RuleViolationException extends RuntimeException { public RuleViolationException(String message) { super(message); } }
}
