package com.example.bandlink.repository;

import com.example.bandlink.entity.LineAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LineAccountRepository extends JpaRepository<LineAccount, Long> {
    Optional<LineAccount> findByLineUserId(String lineUserId);
}
