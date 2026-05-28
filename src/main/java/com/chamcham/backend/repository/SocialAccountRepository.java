package com.chamcham.backend.repository;

import com.chamcham.backend.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {
    List<SocialAccount> findByCreatorId(UUID creatorId);
    void deleteByCreatorIdAndPlatform(UUID creatorId, String platform);
}

