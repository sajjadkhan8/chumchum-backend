package com.zingzing.backend.repository;

import com.zingzing.backend.entity.AuthEmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthEmailVerificationTokenRepository extends JpaRepository<AuthEmailVerificationToken, String> {
}
