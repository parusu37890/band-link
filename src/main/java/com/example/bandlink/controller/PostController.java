package com.example.bandlink.controller;

import com.example.bandlink.dto.PostRequests;
import com.example.bandlink.dto.PostResponse;
import com.example.bandlink.dto.PostSearchCriteria;
import com.example.bandlink.dto.PostPageResponse;
import com.example.bandlink.entity.ActivityFrequency;
import com.example.bandlink.entity.AgeRange;
import com.example.bandlink.repository.UserRepository;
import com.example.bandlink.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;
    private final UserRepository userRepository;
    private final com.example.bandlink.service.SearchHistoryService searchHistoryService;
    private final com.example.bandlink.repository.BlockRepository blocks;

    public PostController(PostService postService, UserRepository userRepository, com.example.bandlink.service.SearchHistoryService searchHistoryService, com.example.bandlink.repository.BlockRepository blocks) {
        this.postService = postService; this.userRepository = userRepository; this.searchHistoryService = searchHistoryService; this.blocks = blocks;
    }

    @PostMapping
    public ResponseEntity<PostResponse> create(Authentication authentication, @Valid @RequestBody PostRequests.Create request) {
        return ResponseEntity.status(201).body(PostResponse.from(postService.create(userId(authentication), request)));
    }

    @PutMapping("/{id}")
    public PostResponse update(Authentication authentication, @PathVariable Long id, @Valid @RequestBody PostRequests.Update request) {
        return PostResponse.from(postService.update(userId(authentication), id, request));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<Void> close(Authentication authentication, @PathVariable Long id) {
        postService.close(userId(authentication), id); return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reopen")
    public PostResponse reopen(Authentication authentication, @PathVariable Long id) {
        return PostResponse.from(postService.reopen(userId(authentication), id));
    }

    @GetMapping
    public List<PostResponse> list(@RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) java.util.Set<Long> prefectureIds,
                                   @RequestParam(required = false) java.util.Set<Long> partIds,
                                   @RequestParam(required = false) java.util.Set<Long> genreIds,
                                   @RequestParam(required = false) java.util.Set<Long> stanceIds,
                                   @RequestParam(required = false) java.util.Set<AgeRange> ageRanges,
                                   @RequestParam(required = false) java.util.Set<ActivityFrequency> activityFrequency,
                                   Authentication authentication) {
        PostSearchCriteria criteria = new PostSearchCriteria(keyword, prefectureIds, partIds, genreIds, stanceIds, ageRanges, activityFrequency);
        if (authentication != null && authentication.isAuthenticated()) userRepository.findByEmail(authentication.getName()).ifPresent(u -> searchHistoryService.record(u.getId(), criteria));
        java.util.Set<Long> hidden = hiddenAuthorIds(authentication);
        return postService.search(criteria).stream().filter(p -> !hidden.contains(p.getUser().getId())).map(PostResponse::from).toList();
    }


    @GetMapping("/mine")
    public List<PostResponse> mine(Authentication authentication) {
        return postService.mine(userId(authentication)).stream().map(PostResponse::from).toList();
    }

    @GetMapping("/page")
    public PostPageResponse page(@RequestParam(required=false) String keyword,
                                 @RequestParam(required=false) java.util.Set<Long> prefectureIds,
                                 @RequestParam(required=false) java.util.Set<Long> partIds,
                                 @RequestParam(required=false) java.util.Set<Long> genreIds,
                                 @RequestParam(required=false) java.util.Set<Long> stanceIds,
                                 @RequestParam(required=false) java.util.Set<AgeRange> ageRanges,
                                 @RequestParam(required=false) java.util.Set<ActivityFrequency> activityFrequency,
                                 @RequestParam(required=false) com.example.bandlink.entity.PostType type,
                                 @RequestParam(required=false) String cursor,
                                 @RequestParam(defaultValue="12") int limit, Authentication authentication) {
        if (limit < 1 || limit > 50) limit = 12;
        PostSearchCriteria criteria = new PostSearchCriteria(keyword, prefectureIds, partIds, genreIds, stanceIds, ageRanges, activityFrequency);
        java.util.Set<Long> hidden = hiddenAuthorIds(authentication);
        List<PostResponse> all = postService.search(criteria).stream().filter(p -> type == null || p.getType().name().equals(type.name())).filter(p -> !hidden.contains(p.getUser().getId())).map(PostResponse::from).toList();
        int offset = 0;
        if (cursor != null && cursor.matches("[0-9]+")) offset = Math.min(Integer.parseInt(cursor), all.size());
        int end = Math.min(offset + limit, all.size());
        boolean hasNext = end < all.size();
        return new PostPageResponse(all.subList(offset,end), hasNext ? String.valueOf(end) : null, hasNext);
    }

    @GetMapping("/{id}")
    public PostResponse detail(@PathVariable Long id) {
        return PostResponse.from(postService.getPublic(id));
    }


    /**
     * Authors the viewer should not see in listings because a block exists in either direction
     * (requirements 8章). The post stays reachable by its own URL: blocking hides someone from
     * browsing, it is not an access control (docs/decisions/0004).
     */
    private java.util.Set<Long> hiddenAuthorIds(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return java.util.Set.of();
        return userRepository.findByEmail(authentication.getName())
                .map(viewer -> blocks.findByBlockerIdOrBlockedId(viewer.getId(), viewer.getId()).stream()
                        .map(block -> block.getBlocker().getId().equals(viewer.getId())
                                ? block.getBlocked().getId() : block.getBlocker().getId())
                        .collect(java.util.stream.Collectors.toSet()))
                .orElseGet(java.util.Set::of);
    }
    private Long userId(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new IllegalStateException("認証ユーザーが見つかりません")).getId();
    }
}
