package com.chamcham.backend.service;

import com.chamcham.backend.entity.Brand;
import com.chamcham.backend.entity.PaymentAuditLog;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.repository.PaymentAuditLogRepository;
import com.chamcham.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentAuditService {

    private static final Logger log = LoggerFactory.getLogger(PaymentAuditService.class);

    private final PaymentAuditLogRepository paymentAuditLogRepository;
    private final UserRepository userRepository;

    public PaymentAuditService(PaymentAuditLogRepository paymentAuditLogRepository, UserRepository userRepository) {
        this.paymentAuditLogRepository = paymentAuditLogRepository;
        this.userRepository = userRepository;
    }

    public void log(UUID actorId, Brand brand, String action, String targetType, String targetId, String details) {
        User actor = userRepository.findById(actorId).orElse(null);
        if (actor == null) {
            // Audit record is permanently lost — log at ERROR so this is visible in monitoring
            log.error("PAYMENT_AUDIT_LOST: actorId={} not found; action={}, targetType={}, targetId={}, details={}",
                    actorId, action, targetType, targetId, details);
            return;
        }

        paymentAuditLogRepository.save(PaymentAuditLog.builder()
                .actor(actor)
                .brand(brand)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .build());
    }
}
