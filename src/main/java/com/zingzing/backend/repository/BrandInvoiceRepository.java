package com.zingzing.backend.repository;

import com.zingzing.backend.entity.BrandInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BrandInvoiceRepository extends JpaRepository<BrandInvoice, UUID> {
    List<BrandInvoice> findByBrandIdOrderByIssuedAtDesc(UUID brandId);
}

