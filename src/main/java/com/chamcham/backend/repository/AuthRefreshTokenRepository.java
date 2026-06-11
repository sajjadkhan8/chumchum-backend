package com.chamcham.backend.repository;

import com.chamcham.backend.entity.AuthRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshToken, String> {
}
