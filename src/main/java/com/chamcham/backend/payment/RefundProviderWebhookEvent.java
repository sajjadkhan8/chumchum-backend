package com.chamcham.backend.payment;

import com.chamcham.backend.entity.enums.TransactionStatus;

public record RefundProviderWebhookEvent(
        String provider,
        String providerRefundId,
        TransactionStatus status,
        String failureReason
) {}
