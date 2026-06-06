package com.chamcham.backend.repository;

import com.chamcham.backend.entity.BrandDisbursement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BrandDisbursementRepository extends JpaRepository<BrandDisbursement, UUID> {
    List<BrandDisbursement> findByBrandIdOrderByReleaseDateDesc(UUID brandId);
}

