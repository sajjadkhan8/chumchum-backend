package com.zingzing.backend.dto.order;

import jakarta.validation.constraints.NotBlank;

public record PaymentConfirmRequest(
        @NotBlank String paymentIntent
) {
}

