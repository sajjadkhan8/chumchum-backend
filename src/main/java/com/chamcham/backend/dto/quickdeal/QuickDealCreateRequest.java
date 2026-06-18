package com.chamcham.backend.dto.quickdeal;

import com.chamcham.backend.entity.enums.DealType;
import com.chamcham.backend.entity.enums.PackagePlatform;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record QuickDealCreateRequest(
        @NotNull UUID creatorId,
        @NotNull DealType dealType,
        @Min(0) Integer amount,
        @Size(max = 2000) String barterDetails,
        @Size(max = 100) String barterCategory,
        @Min(0) Integer estimatedBarterValue,
        @Size(max = 2000) String creatorExpectation,
        @NotNull @Size(min = 1, max = 1000) String message,
        PackagePlatform platform,
        @Min(1) Integer deliveryDays
) {
}

