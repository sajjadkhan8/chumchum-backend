package com.zingzing.backend.repository;

import com.zingzing.backend.entity.SocialOAuthState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialOAuthStateRepository extends JpaRepository<SocialOAuthState, String> {
}
