package com.chamcham.backend.dto.order;

public record PaymentIntentResponse(
        boolean error,
        String clientSecret
) {
}

