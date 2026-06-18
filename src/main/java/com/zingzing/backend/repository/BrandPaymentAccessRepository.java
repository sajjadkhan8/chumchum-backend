package com.zingzing.backend.repository;

import com.zingzing.backend.entity.BrandPaymentAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BrandPaymentAccessRepository extends JpaRepository<BrandPaymentAccess, UUID> {
    Optional<BrandPaymentAccess> findByBrandIdAndUserId(UUID brandId, UUID userId);
    List<BrandPaymentAccess> findByUserId(UUID userId);
}

