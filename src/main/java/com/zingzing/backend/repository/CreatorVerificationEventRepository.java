package com.zingzing.backend.repository;

import com.zingzing.backend.entity.CreatorVerificationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CreatorVerificationEventRepository extends JpaRepository<CreatorVerificationEvent, UUID> {
    List<CreatorVerificationEvent> findByCreatorIdOrderByCreatedAtDesc(UUID creatorId);
}
