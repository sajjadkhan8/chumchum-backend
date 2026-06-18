package com.zingzing.backend.repository;

import com.zingzing.backend.entity.AuthRateLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthRateLimitRepository extends JpaRepository<AuthRateLimit, UUID> {
    Optional<AuthRateLimit> findByActionAndIdentifier(String action, String identifier);
}
