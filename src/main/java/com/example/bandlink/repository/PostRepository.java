package com.example.bandlink.repository;

import com.example.bandlink.entity.Post;
import com.example.bandlink.entity.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    boolean existsByUserIdAndStatus(Long userId, PostStatus status);
    Optional<Post> findByIdAndUserId(Long id, Long userId);
}
