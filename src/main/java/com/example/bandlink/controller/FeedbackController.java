package com.example.bandlink.controller;

import com.example.bandlink.dto.FeedbackRequest;
import com.example.bandlink.entity.FeedbackType;
import com.example.bandlink.repository.UserRepository;
import com.example.bandlink.service.FeedbackService;
import com.example.bandlink.service.ImageStorageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
    private final FeedbackService service;
    private final UserRepository users;
    private final ImageStorageService storage;

    public FeedbackController(FeedbackService service, UserRepository users, ImageStorageService storage) {
        this.service = service;
        this.users = users;
        this.storage = storage;
    }

    @PostMapping("/contact")
    @ResponseStatus(HttpStatus.CREATED)
    public void contact(Authentication authentication, @Valid @RequestBody FeedbackRequest request) {
        service.create(current(authentication), FeedbackType.CONTACT, request);
    }

    @PostMapping("/feature-request")
    @ResponseStatus(HttpStatus.CREATED)
    public void featureRequest(Authentication authentication, @Valid @RequestBody FeedbackRequest request) {
        service.create(current(authentication), FeedbackType.FEATURE_REQUEST, request);
    }

    @PostMapping(value = "/images", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public String image(Authentication authentication, @RequestParam MultipartFile file) {
        current(authentication);
        // SEC-011: not store() - a feedback screenshot can show account details or error content
        // that shouldn't be reachable by anyone who has (or guesses) the URL, only by the admin
        // reviewing the report. storeFeedback() puts it under a directory this same file's
        // sibling admin-only endpoint serves, not the public /uploads/ tree.
        return storage.storeFeedback(file);
    }

    private Long current(Authentication authentication) {
        return users.findByEmail(authentication.getName()).orElseThrow().getId();
    }
}
