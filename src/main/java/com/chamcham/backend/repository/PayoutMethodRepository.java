package com.chamcham.backend.repository;

import com.chamcham.backend.entity.PayoutMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayoutMethodRepository extends JpaRepository<PayoutMethod, UUID> {
    List<PayoutMethod> findByCreatorId(UUID creatorId);
    Optional<PayoutMethod> findByCreatorIdAndIsDefaultTrue(UUID creatorId);
}

