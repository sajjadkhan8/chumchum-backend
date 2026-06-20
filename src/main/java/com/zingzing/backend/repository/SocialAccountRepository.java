package com.zingzing.backend.repository;

import com.zingzing.backend.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {
    List<SocialAccount> findByCreatorId(UUID creatorId);
    Optional<SocialAccount> findByCreatorIdAndPlatformIgnoreCase(UUID creatorId, String platform);
    void deleteByCreatorIdAndPlatform(UUID creatorId, String platform);
}
