package com.chamcham.backend.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.UUID;

public record MessageCreateRequest(
        @NotNull UUID conversationId,
        String type,
        String content,
        @NotBlank String description
        String attachmentUrl,
        String offerDealType,
        @DecimalMin("0.00") BigDecimal offerAmount,
        String offerBarterDetails,
        String offerBarterCategory,
        @DecimalMin("0.00") BigDecimal offerEstimatedBarterValue,
        String offerCreatorExpectation,
        String offerMessage
) {
}

