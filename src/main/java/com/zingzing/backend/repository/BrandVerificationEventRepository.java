package com.zingzing.backend.repository;

import com.zingzing.backend.entity.BrandVerificationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BrandVerificationEventRepository extends JpaRepository<BrandVerificationEvent, UUID> {
    List<BrandVerificationEvent> findByBrandIdOrderByCreatedAtDesc(UUID brandId);
}
