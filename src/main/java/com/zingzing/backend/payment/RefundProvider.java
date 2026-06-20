package com.zingzing.backend.payment;

import java.util.UUID;

public interface RefundProvider {
    String providerName();
    boolean verifyWebhookSignature(String signature);
    RefundSubmission submit(RefundRequest request);

    record RefundRequest(UUID refundId, UUID orderId, int amount, String reason, String providerPaymentId, String trackerToken) {}
    record RefundSubmission(String providerRefundId, String providerPaymentId, String providerResponse) {}
}
