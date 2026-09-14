package com.example.bandlink.repository;

import com.example.bandlink.entity.Feedback;
import com.example.bandlink.entity.FeedbackType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByTypeOrderByCreatedAtAsc(FeedbackType type);
    List<Feedback> findAllByOrderByCreatedAtDesc();
}
