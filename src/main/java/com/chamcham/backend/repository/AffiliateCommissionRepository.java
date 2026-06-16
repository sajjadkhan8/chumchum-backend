package com.chamcham.backend.repository;

import com.chamcham.backend.entity.AffiliateCommission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AffiliateCommissionRepository extends JpaRepository<AffiliateCommission, UUID> {
    Page<AffiliateCommission> findByAffiliateOwnerIdOrderByCreatedAtDesc(UUID affiliateOwnerId, Pageable pageable);
    Optional<AffiliateCommission> findByOrderId(UUID orderId);
    boolean existsByOrderId(UUID orderId);
    long countByAffiliateOwnerId(UUID affiliateOwnerId);

    @Query("select coalesce(sum(c.commissionAmount), 0) from AffiliateCommission c where c.affiliateOwner.id = :ownerId")
    long sumCommissionAmountByOwner(@Param("ownerId") UUID ownerId);
}
