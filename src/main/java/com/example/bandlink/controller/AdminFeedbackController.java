package com.example.bandlink.controller;

import com.example.bandlink.dto.FeedbackResponse;
import com.example.bandlink.repository.FeedbackRepository;
import com.example.bandlink.service.ImageStorageService;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/feedback")
@org.springframework.transaction.annotation.Transactional
public class AdminFeedbackController {
    private final FeedbackRepository feedback;
    private final ImageStorageService storage;

    public AdminFeedbackController(FeedbackRepository feedback, ImageStorageService storage) {
        this.feedback = feedback;
        this.storage = storage;
    }

    @GetMapping
    public List<FeedbackResponse> list() {
        return feedback.findAllByOrderByCreatedAtDesc().stream().map(FeedbackResponse::from).toList();
    }

    // SEC-011: the only route to this file's bytes - SecurityConfig gates all of /api/admin/**
    // (this endpoint included) on hasRole("ADMIN"), so reaching this method at all already proves
    // the caller is an admin; no separate per-image ownership check is needed the way DM images
    // need one (any admin may review any feedback attachment).
    @GetMapping("/images/{name}")
    public ResponseEntity<Resource> image(@PathVariable String name) {
        String type = name.endsWith(".png") ? "image/png" : name.endsWith(".webp") ? "image/webp" : "image/jpeg";
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(type)).body(storage.loadFeedback(name));
    }
}
