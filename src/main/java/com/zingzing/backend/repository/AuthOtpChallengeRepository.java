package com.zingzing.backend.repository;

import com.zingzing.backend.entity.AuthOtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthOtpChallengeRepository extends JpaRepository<AuthOtpChallenge, String> {
}
