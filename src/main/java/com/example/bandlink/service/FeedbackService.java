package com.example.bandlink.service;

import com.example.bandlink.dto.FeedbackRequest;
import com.example.bandlink.entity.Feedback;
import com.example.bandlink.entity.FeedbackType;
import com.example.bandlink.repository.FeedbackRepository;
import com.example.bandlink.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedbackService {
    private final UserRepository users;
    private final FeedbackRepository feedback;
    private final Clock clock;
    private final MailService mail;

    @Autowired
    public FeedbackService(UserRepository users, FeedbackRepository feedback, MailService mail) {
        this(users, feedback, Clock.systemDefaultZone(), mail);
    }

    FeedbackService(UserRepository users, FeedbackRepository feedback, Clock clock, MailService mail) {
        this.users = users;
        this.feedback = feedback;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
        this.mail = mail;
    }

    @Transactional
    public Feedback create(Long userId, FeedbackType type, FeedbackRequest request) {
        if (type == null) throw new IllegalArgumentException("送信内容の種類を確認してください。");
        String message = request.message() == null ? "" : request.message().trim();
        if (message.isBlank()) throw new IllegalArgumentException("内容を入力してください。");
        String image = request.imageUrl();
        // SEC-011: this used to accept only the public /uploads/ shape, which is exactly the
        // shape ImageStorageService.storeFeedback() (see FeedbackController.image()) stopped
        // producing - it now returns /api/admin/feedback/images/{name}, the private path this
        // controller's sibling admin-only endpoint serves. Leaving the old pattern here would
        // have rejected every legitimately-uploaded attachment with "添付画像を確認してください。"
        if (image != null && !image.isBlank()
                && !image.matches("/api/admin/feedback/images/[A-Za-z0-9-]+\\.(jpg|png|webp)")) {
            throw new IllegalArgumentException("添付画像を確認してください。");
        }
        var user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません。"));
        Feedback saved = feedback.save(new Feedback(user, type, message,
                image == null || image.isBlank() ? null : image, LocalDateTime.now(clock)));
        mail.sendFeedbackNotification(type, user.getUsername(), user.getEmail(), message);
        return saved;
    }
}
