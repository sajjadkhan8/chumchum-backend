package com.chamcham.backend.repository;

import com.chamcham.backend.entity.AuthPasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthPasswordResetTokenRepository extends JpaRepository<AuthPasswordResetToken, String> {
}
