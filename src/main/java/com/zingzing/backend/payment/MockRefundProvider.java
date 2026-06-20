package com.zingzing.backend.payment;

import com.zingzing.backend.entity.enums.TransactionStatus;
import com.zingzing.backend.config.RefundProperties;
import com.zingzing.backend.config.SafepayProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "app.refunds.provider", havingValue = "mock", matchIfMissing = true)
public class MockRefundProvider implements RefundProvider {

    private final ApplicationEventPublisher eventPublisher;
    private final String webhookSecret;

    public MockRefundProvider(ApplicationEventPublisher eventPublisher,
                              @Value("${app.refunds.mock.webhook-secret}") String webhookSecret,
                              RefundProperties refundProperties,
                              SafepayProperties safepayProperties) {
        if (safepayProperties.isProduction() && !refundProperties.isAllowMockInProduction()) {
            throw new IllegalStateException("Mock refund provider is disabled when SAFEPAY_ENVIRONMENT=production");
        }
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
        return new RefundSubmission(providerRefundId, null, "{\"provider\":\"mock\"}");
    }
}
