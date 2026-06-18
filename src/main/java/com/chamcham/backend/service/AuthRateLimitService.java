package com.chamcham.backend.service;

import com.chamcham.backend.entity.AuthRateLimit;
import com.chamcham.backend.repository.AuthRateLimitRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthRateLimitService {

    private final AuthRateLimitRepository repository;

    public AuthRateLimitService(AuthRateLimitRepository repository) {
        this.repository = repository;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public boolean recordAndCheck(String action, String identifier, int maxAttempts, long windowMinutes, long blockMinutes) {
        Instant now = Instant.now();
        AuthRateLimit limit = repository.findByActionAndIdentifier(action, identifier)
                .orElseGet(() -> AuthRateLimit.builder()
                        .action(action)
                        .identifier(identifier)
                        .attempts(0)
                        .windowStartedAt(now)
                        .build());

        if (limit.getBlockedUntil() != null && limit.getBlockedUntil().isAfter(now)) {
            return true;
        }
        if (limit.getWindowStartedAt().plus(windowMinutes, ChronoUnit.MINUTES).isBefore(now)) {
            limit.setAttempts(0);
            limit.setWindowStartedAt(now);
            limit.setBlockedUntil(null);
        }

        limit.setAttempts(limit.getAttempts() + 1);
        if (limit.getAttempts() >= maxAttempts) {
            limit.setBlockedUntil(now.plus(blockMinutes, ChronoUnit.MINUTES));
        }
        repository.save(limit);
        return limit.getBlockedUntil() != null;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void clear(String action, String identifier) {
        repository.findByActionAndIdentifier(action, identifier).ifPresent(repository::delete);
    }
}
