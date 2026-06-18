package com.zingzing.backend.repository;

import com.zingzing.backend.entity.BrandPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BrandPaymentMethodRepository extends JpaRepository<BrandPaymentMethod, UUID> {
    List<BrandPaymentMethod> findByBrandIdOrderByCreatedAtDesc(UUID brandId);
    Optional<BrandPaymentMethod> findByBrandIdAndIsDefaultTrue(UUID brandId);
}

