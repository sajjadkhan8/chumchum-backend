package com.zingzing.backend.payment;

import com.zingzing.backend.entity.SafepayPaymentSession;
import com.zingzing.backend.entity.enums.SafepayPaymentStatus;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.repository.SafepayPaymentSessionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.refunds.provider", havingValue = "safepay")
public class SafepayRefundProvider implements RefundProvider {

    private final SafepayClient safepayClient;
    private final SafepayPaymentSessionRepository sessionRepository;

    public SafepayRefundProvider(SafepayClient safepayClient, SafepayPaymentSessionRepository sessionRepository) {
        this.safepayClient = safepayClient;
        this.sessionRepository = sessionRepository;
    }

    @Override
    public String providerName() {
        return "safepay";
    }

    @Override
    public boolean verifyWebhookSignature(String signature) {
        return signature != null && !signature.isBlank();
    }

    @Override
    public RefundSubmission submit(RefundRequest request) {
        SafepayPaymentSession session = sessionRepository
                .findFirstByOrderIdAndStatusOrderByCreatedAtDesc(request.orderId(), SafepayPaymentStatus.COMPLETED)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No completed Safepay payment session found for this order"));
        String providerPaymentId = session.getSafepayPaymentRef() == null || session.getSafepayPaymentRef().isBlank()
                ? session.getTrackerToken()
                : session.getSafepayPaymentRef();
        SafepayClient.RefundResult result = safepayClient.createRefund(
                session.getTrackerToken(),
                session.getSafepayPaymentRef(),
                request.amount(),
                request.reason(),
                Map.of(
                        "order_id", request.orderId().toString(),
                        "payment_session_id", session.getId().toString(),
                        "tracker", session.getTrackerToken()
                )
        );
        return new RefundSubmission(result.refundId(), providerPaymentId, result.rawResponse());
    }
}
