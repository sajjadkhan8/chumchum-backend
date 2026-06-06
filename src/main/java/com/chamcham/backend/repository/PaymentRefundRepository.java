package com.chamcham.backend.repository;

import com.chamcham.backend.entity.PaymentRefund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, UUID> {
    boolean existsByDisputeId(UUID disputeId);
    Optional<PaymentRefund> findByProviderRefundId(String providerRefundId);
}
