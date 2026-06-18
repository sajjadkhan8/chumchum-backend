package com.zingzing.backend.payment;

import com.zingzing.backend.entity.enums.TransactionStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class MockRefundProvider implements RefundProvider {

    private final ApplicationEventPublisher eventPublisher;
    private final String webhookSecret;

    public MockRefundProvider(ApplicationEventPublisher eventPublisher,
                              @Value("${app.refunds.mock.webhook-secret}") String webhookSecret) {
        this.eventPublisher = eventPublisher;
        this.webhookSecret = webhookSecret;
    }

    @Override
    public String providerName() {
        return "mock";
    }

    @Override
    public boolean verifyWebhookSignature(String signature) {
        return webhookSecret.equals(signature);
    }

    @Override
    public RefundSubmission submit(RefundRequest request) {
        String providerRefundId = "mock_refund_" + UUID.randomUUID();
        boolean simulateFailure = request.reason().toLowerCase().contains("[mock-fail]");
        CompletableFuture.delayedExecutor(1200, TimeUnit.MILLISECONDS).execute(() ->
                eventPublisher.publishEvent(new RefundProviderWebhookEvent(
                        providerName(),
                        providerRefundId,
                        simulateFailure ? TransactionStatus.FAILED : TransactionStatus.COMPLETED,
                        simulateFailure ? "Mock provider declined the refund" : null
                )));
        return new RefundSubmission(providerRefundId);
    }
}
