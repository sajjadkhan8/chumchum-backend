package com.zingzing.backend.repository;

import com.zingzing.backend.entity.AuthRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshToken, String> {

    @Modifying
    @Query("update AuthRefreshToken t set t.revokedAt = :now where t.user.id = :userId and t.revokedAt is null")
    void revokeAllByUserId(@Param("userId") UUID userId, @Param("now") Instant now);
}
