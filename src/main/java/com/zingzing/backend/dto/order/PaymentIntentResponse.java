package com.zingzing.backend.dto.order;

public record PaymentIntentResponse(
        boolean error,
        String clientSecret
) {
}

