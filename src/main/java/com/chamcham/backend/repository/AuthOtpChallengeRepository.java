package com.chamcham.backend.repository;

import com.chamcham.backend.entity.AuthOtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthOtpChallengeRepository extends JpaRepository<AuthOtpChallenge, String> {
}
