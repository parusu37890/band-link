package com.example.bandlink.controller;

import com.example.bandlink.dto.FeedbackResponse;
import com.example.bandlink.repository.FeedbackRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/feedback")
public class AdminFeedbackController {
    private final FeedbackRepository feedback;

    public AdminFeedbackController(FeedbackRepository feedback) {
        this.feedback = feedback;
    }

    @GetMapping
    public List<FeedbackResponse> list() {
        return feedback.findAllByOrderByCreatedAtDesc().stream().map(FeedbackResponse::from).toList();
    }
}
