package com.zingzing.backend.payment;

import com.zingzing.backend.entity.enums.TransactionStatus;

public record RefundProviderWebhookEvent(
        String provider,
        String providerRefundId,
        TransactionStatus status,
        String failureReason
) {}
