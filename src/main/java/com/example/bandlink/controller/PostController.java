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
@org.springframework.transaction.annotation.Transactional
public class PostController {
    private final PostService postService;
    private final UserRepository userRepository;
    private final com.example.bandlink.service.SearchHistoryService searchHistoryService;

    public PostController(PostService postService, UserRepository userRepository, com.example.bandlink.service.SearchHistoryService searchHistoryService) {
        this.postService = postService; this.userRepository = userRepository; this.searchHistoryService = searchHistoryService;
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

    @PatchMapping("/{id}/boost")
    public PostResponse boost(Authentication authentication, @PathVariable Long id) {
        return PostResponse.from(postService.boost(userId(authentication), id));
    }

    @GetMapping
    public List<PostResponse> list(@RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) java.util.Set<Long> prefectureIds,
                                   @RequestParam(required = false) java.util.Set<Long> partIds,
                                   @RequestParam(required = false) java.util.Set<Long> genreIds,
                                   @RequestParam(required = false) java.util.Set<Long> stanceIds,
                                   @RequestParam(required = false) java.util.Set<AgeRange> ageRanges,
                                   @RequestParam(required = false) java.util.Set<ActivityFrequency> activityFrequency,
                                   @RequestParam(defaultValue = "recent") String sort,
                                   Authentication authentication) {
        PostSearchCriteria criteria = new PostSearchCriteria(keyword, prefectureIds, partIds, genreIds, stanceIds, ageRanges, activityFrequency);
        if (authentication != null && authentication.isAuthenticated()) userRepository.findByEmail(authentication.getName()).ifPresent(u -> searchHistoryService.record(u.getId(), criteria));
        return postService.searchFor(viewerId(authentication), criteria, sort).stream().map(PostResponse::from).toList();
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
                                 @RequestParam(defaultValue="recent") String sort,
                                 @RequestParam(required=false) String cursor,
                                 @RequestParam(defaultValue="12") int limit, Authentication authentication) {
        if (limit < 1 || limit > 50) limit = 12;
        PostSearchCriteria criteria = new PostSearchCriteria(keyword, prefectureIds, partIds, genreIds, stanceIds, ageRanges, activityFrequency);
        // This cursor-paginated endpoint is what the actual search screen calls (see recruitment-search.js);
        // the plain GET /api/posts above is not used by the frontend for searching at all, so recording
        // history only there meant a signed-in person's real searches were never saved. Record only on the
        // cursor-less first page so "load more" pages of the same search don't spam the history; the
        // service itself already no-ops for an unconditioned search (hasConditions()==false).
        if (cursor == null && authentication != null && authentication.isAuthenticated())
            userRepository.findByEmail(authentication.getName()).ifPresent(u -> searchHistoryService.record(u.getId(), criteria));
        List<PostResponse> all = postService.searchFor(viewerId(authentication), criteria, sort).stream().filter(p -> type == null || p.getType().name().equals(type.name())).map(PostResponse::from).toList();
        int offset = resolveCursorOffset(cursor, all.size());
        int end = Math.min(offset + limit, all.size());
        boolean hasNext = end < all.size();
        return new PostPageResponse(all.subList(offset,end), hasNext ? String.valueOf(end) : null, hasNext);
    }

    /**
     * Resolves the "/page" cursor into a list offset. Extracted as a pure function (no DB access)
     * so the release case matrix can exercise the huge-cursor/invalid-cursor scenarios directly:
     * a cursor that isn't all digits is treated as "start from the top" rather than an error, and
     * a cursor too large to parse as an int is clamped to the end of the list rather than throwing.
     */
    public static int resolveCursorOffset(String cursor, int total) {
        if (cursor == null || !cursor.matches("[0-9]+")) return 0;
        try { return Math.min(Math.max(0, Integer.parseInt(cursor)), total); }
        catch (NumberFormatException ignored) { return total; }
    }

    @GetMapping("/{id}")
    public PostResponse detail(@PathVariable Long id) {
        return PostResponse.from(postService.getPublic(id));
    }


    /** The signed-in viewer, or null when browsing anonymously. */
    private Long viewerId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        return userRepository.findByEmail(authentication.getName()).map(com.example.bandlink.entity.User::getId).orElse(null);
    }
    private Long userId(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new IllegalStateException("認証ユーザーが見つかりません")).getId();
    }
}
