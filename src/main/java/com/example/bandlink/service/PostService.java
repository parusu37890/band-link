package com.example.bandlink.service;

import com.example.bandlink.dto.PostRequests;
import com.example.bandlink.dto.PostSearchCriteria;
import com.example.bandlink.entity.*;
import com.example.bandlink.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
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
    private final BlockRepository blockRepository;
    private final Clock clock;
    @org.springframework.beans.factory.annotation.Autowired
    public PostService(PostRepository posts, UserRepository users, PartRepository parts, GenreRepository genres,
                       StanceRepository stances, PrefectureRepository prefectures, BlockRepository blocks) {
        this(posts, users, parts, genres, stances, prefectures, blocks, Clock.systemDefaultZone());
    }
    PostService(PostRepository posts, UserRepository users, PartRepository parts, GenreRepository genres,
                StanceRepository stances, PrefectureRepository prefectures, BlockRepository blocks, Clock clock) {
        this.postRepository = posts; this.userRepository = users; this.partRepository = parts; this.genreRepository = genres;
        this.stanceRepository = stances; this.prefectureRepository = prefectures; this.blockRepository = blocks;
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
    /**
     * Listing for a signed-in viewer. Posts by anyone in a block relationship with them are excluded
     * in the query (requirements 8章): blocking hides people from browsing, while the post's own URL
     * stays reachable (docs/decisions/0004). Anonymous browsing uses {@link #search} and sees
     * everything, which is why blocking is not an access control.
     */
    public List<Post> searchFor(Long viewerId, PostSearchCriteria criteria) {
        java.util.Set<Long> hidden = blockedCounterparts(viewerId);
        List<Post> found = search(criteria);
        return hidden.isEmpty() ? found
                : found.stream().filter(post -> !hidden.contains(post.getUser().getId())).toList();
    }

    /** Ids on the other side of a block, whichever direction it was made in. */
    public java.util.Set<Long> blockedCounterparts(Long viewerId) {
        if (viewerId == null) return java.util.Set.of();
        return blockRepository.findByBlockerIdOrBlockedId(viewerId, viewerId).stream()
                .map(block -> block.getBlocker().getId().equals(viewerId)
                        ? block.getBlocked().getId() : block.getBlocker().getId())
                .collect(java.util.stream.Collectors.toSet());
    }

    public List<Post> search(PostSearchCriteria criteria) {
        LocalDateTime now = now();
        Specification<Post> spec = (root, query, cb) -> cb.and(cb.equal(root.get("status"), PostStatus.OPEN), cb.equal(root.get("user").get("status"), UserStatus.ACTIVE));
        if (criteria != null && criteria.keyword() != null && !criteria.keyword().isBlank()) {
            String keyword = "%" + criteria.keyword().trim().toLowerCase(java.util.Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(cb.like(cb.lower(root.get("title")), keyword), cb.like(cb.lower(root.get("content")), keyword)));
        }
        if (criteria != null && criteria.activityFrequencies() != null && !criteria.activityFrequencies().isEmpty())
            spec = spec.and((root, query, cb) -> root.get("activityFrequency").in(criteria.activityFrequencies()));
        if (criteria != null && criteria.ageRanges() != null && !criteria.ageRanges().isEmpty())
            spec = spec.and((root, query, cb) -> root.join("ageRanges").in(criteria.ageRanges()));
        spec = relationFilter(spec, "prefectures", criteria == null ? null : criteria.prefectureIds());
        spec = relationFilter(spec, "parts", criteria == null ? null : criteria.partIds());
        spec = relationFilter(spec, "genres", criteria == null ? null : criteria.genreIds());
        spec = relationFilter(spec, "stances", criteria == null ? null : criteria.stanceIds());
        spec = spec.and((root, query, cb) -> { query.distinct(true); return cb.conjunction(); });
        return postRepository.findAll(spec, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "rankUpdatedAt")).stream()
                .peek(post -> expireIfNeeded(post, now)).filter(post -> post.getStatus() == PostStatus.OPEN).toList();
    }

    private Specification<Post> relationFilter(Specification<Post> base, String relation, java.util.Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return base;
        return base.and((root, query, cb) -> root.join(relation).get("id").in(ids));
    }

    @Transactional
    public Post getPublic(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuleViolationException("投稿が見つかりません"));
        expireIfNeeded(post, now());
        if (!post.getUser().isActive() || (post.getStatus() == PostStatus.CLOSED
                && post.getClosedReason() != ClosedReason.MANUAL && post.getClosedReason() != ClosedReason.EXPIRED))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "投稿が見つかりません");
        return post;
    }

    @Transactional
    public List<Post> mine(Long userId) {
        activeUser(userId);
        List<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId);
        posts.forEach(post -> expireIfNeeded(post, now()));
        return posts;
    }

    @Transactional
    public void adminDelete(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuleViolationException("投稿が見つかりません"));
        post.close(ClosedReason.DELETED_BY_ADMIN, now());
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
