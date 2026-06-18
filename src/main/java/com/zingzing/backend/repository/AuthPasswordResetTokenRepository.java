package com.zingzing.backend.repository;

import com.zingzing.backend.entity.AuthPasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthPasswordResetTokenRepository extends JpaRepository<AuthPasswordResetToken, String> {
}
