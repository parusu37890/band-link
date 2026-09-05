package com.example.bandlink.controller;

import com.example.bandlink.dto.PostImageResponse;
import com.example.bandlink.repository.UserRepository;
import com.example.bandlink.service.PostImageService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/posts/{postId}/images")
public class PostImageController {
    private final PostImageService service;
    private final UserRepository users;
    public PostImageController(PostImageService service, UserRepository users) { this.service=service; this.users=users; }
    @GetMapping public List<PostImageResponse> list(@PathVariable Long postId) { return service.list(postId).stream().map(PostImageResponse::from).toList(); }
    @PostMapping public ResponseEntity<PostImageResponse> add(Authentication a,@PathVariable Long postId,@RequestParam MultipartFile file) { return ResponseEntity.status(201).body(PostImageResponse.from(service.add(current(a),postId,file))); }
    @PostMapping("/batch") public ResponseEntity<List<PostImageResponse>> addAll(Authentication a,@PathVariable Long postId,@RequestParam("files") List<MultipartFile> files) { return ResponseEntity.status(201).body(service.addAll(current(a),postId,files).stream().map(PostImageResponse::from).toList()); }
    @DeleteMapping("/{imageId}") public ResponseEntity<Void> remove(Authentication a,@PathVariable Long postId,@PathVariable Long imageId) { service.remove(current(a),postId,imageId); return ResponseEntity.noContent().build(); }
    @PatchMapping public ResponseEntity<Void> reorder(Authentication a,@PathVariable Long postId,@RequestBody List<Long> imageIds) { service.reorder(current(a),postId,imageIds); return ResponseEntity.noContent().build(); }
    private Long current(Authentication a){return users.findByEmail(a.getName()).orElseThrow().getId();}
}
