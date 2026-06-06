package com.chamcham.backend.repository;

import com.chamcham.backend.entity.PaymentAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentAuditLogRepository extends JpaRepository<PaymentAuditLog, UUID> {
}

