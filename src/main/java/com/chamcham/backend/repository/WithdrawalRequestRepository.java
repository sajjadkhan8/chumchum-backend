package com.chamcham.backend.repository;

import com.chamcham.backend.entity.WithdrawalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, UUID> {
    Page<WithdrawalRequest> findByCreatorIdOrderByCreatedAtDesc(UUID creatorId, Pageable pageable);
}

