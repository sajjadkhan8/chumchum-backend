package com.chamcham.backend.service;

import com.chamcham.backend.entity.Brand;
import com.chamcham.backend.entity.PaymentAuditLog;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.repository.PaymentAuditLogRepository;
import com.chamcham.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentAuditService {

    private final PaymentAuditLogRepository paymentAuditLogRepository;
    private final UserRepository userRepository;

    public PaymentAuditService(PaymentAuditLogRepository paymentAuditLogRepository, UserRepository userRepository) {
        this.paymentAuditLogRepository = paymentAuditLogRepository;
        this.userRepository = userRepository;
    }

    public void log(UUID actorId, Brand brand, String action, String targetType, String targetId, String details) {
        User actor = userRepository.findById(actorId).orElse(null);
        if (actor == null) return;

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

