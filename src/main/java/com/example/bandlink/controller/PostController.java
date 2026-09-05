package com.example.bandlink.controller;

import com.example.bandlink.dto.PostRequests;
import com.example.bandlink.dto.PostResponse;
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

    public PostController(PostService postService, UserRepository userRepository) {
        this.postService = postService; this.userRepository = userRepository;
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
    public List<PostResponse> list() {
        return postService.listOpen().stream().map(PostResponse::from).toList();
    }

    @GetMapping("/{id}")
    public PostResponse detail(@PathVariable Long id) {
        return PostResponse.from(postService.getPublic(id));
    }

    private Long userId(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new IllegalStateException("認証ユーザーが見つかりません")).getId();
    }
}
