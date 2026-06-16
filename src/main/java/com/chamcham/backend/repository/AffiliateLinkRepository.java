package com.chamcham.backend.repository;

import com.chamcham.backend.entity.AffiliateLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AffiliateLinkRepository extends JpaRepository<AffiliateLink, UUID> {
    Optional<AffiliateLink> findByOwnerId(UUID ownerId);
    Optional<AffiliateLink> findByCodeIgnoreCaseAndActiveTrue(String code);
    boolean existsByCodeIgnoreCase(String code);
}
