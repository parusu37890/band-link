package com.example.bandlink.repository;

import com.example.bandlink.entity.XAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface XAccountRepository extends JpaRepository<XAccount, Long> {
    Optional<XAccount> findByXUserId(String xUserId);
}
