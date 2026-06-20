package com.zingzing.backend.repository;

import com.zingzing.backend.entity.BrandVerificationDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BrandVerificationDocumentRepository extends JpaRepository<BrandVerificationDocument, UUID> {
    List<BrandVerificationDocument> findByBrandIdOrderByUploadedAtDesc(UUID brandId);
}
