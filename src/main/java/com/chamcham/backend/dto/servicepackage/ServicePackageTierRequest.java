package com.chamcham.backend.dto.servicepackage;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ServicePackageTierRequest(
        @Size(max = 50) String name,
        @DecimalMin("1.00") BigDecimal price,
        @Size(max = 1000) String deliverables,
        @Positive Integer deliveryDays,
        @Positive Integer revisions
) {
}

