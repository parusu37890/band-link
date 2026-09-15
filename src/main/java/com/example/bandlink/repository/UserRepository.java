package com.example.bandlink.repository;

import com.example.bandlink.entity.User;
import com.example.bandlink.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByStatusOrderByIdAsc(UserStatus status);
}
