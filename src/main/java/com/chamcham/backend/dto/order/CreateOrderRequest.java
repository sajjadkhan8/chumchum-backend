package com.chamcham.backend.dto.order;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreateOrderRequest(
        @NotNull UUID packageId,
        @NotNull UUID brandId,
        String dealType,
        @DecimalMin("0.00") BigDecimal amount,
        String barterDetails,
        String message
) {
}

