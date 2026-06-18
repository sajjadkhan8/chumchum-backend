package com.zingzing.backend.repository;

import com.zingzing.backend.entity.ContentPreview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContentPreviewRepository extends JpaRepository<ContentPreview, UUID> {
    List<ContentPreview> findByCreatorIdOrderByCreatedAtDesc(UUID creatorId);
}

