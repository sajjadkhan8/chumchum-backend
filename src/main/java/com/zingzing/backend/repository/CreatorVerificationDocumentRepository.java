package com.zingzing.backend.repository;

import com.zingzing.backend.entity.CreatorVerificationDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreatorVerificationDocumentRepository extends JpaRepository<CreatorVerificationDocument, UUID> {
    List<CreatorVerificationDocument> findByCreatorIdOrderByUploadedAtDesc(UUID creatorId);
    Optional<CreatorVerificationDocument> findByIdAndCreatorId(UUID id, UUID creatorId);
}
