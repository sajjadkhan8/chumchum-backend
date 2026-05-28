package com.chamcham.backend.repository;

import com.chamcham.backend.entity.AmbassadorApplication;
import com.chamcham.backend.entity.enums.AmbassadorAppStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AmbassadorApplicationRepository extends JpaRepository<AmbassadorApplication, UUID> {
    Optional<AmbassadorApplication> findByCreatorId(UUID creatorId);
    Page<AmbassadorApplication> findByStatus(AmbassadorAppStatus status, Pageable pageable);
}

