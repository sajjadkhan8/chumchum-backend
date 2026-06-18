package com.zingzing.backend.dto.order;

import com.zingzing.backend.entity.enums.DealType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateOrderRequest(
        @NotNull UUID packageId,
        DealType dealType,
        @Min(0) @Max(50_000_000) Integer amount,
        @Size(max = 2000) String barterDetails,
        @Size(max = 2000) String message
) {
}
