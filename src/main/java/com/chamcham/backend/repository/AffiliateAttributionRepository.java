package com.chamcham.backend.repository;

import com.chamcham.backend.entity.AffiliateAttribution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AffiliateAttributionRepository extends JpaRepository<AffiliateAttribution, UUID> {
    Optional<AffiliateAttribution> findByReferredCreatorId(UUID referredCreatorId);
    long countByAffiliateOwnerId(UUID affiliateOwnerId);
    boolean existsByReferredCreatorId(UUID referredCreatorId);
}
