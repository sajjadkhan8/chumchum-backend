package com.chamcham.backend.dto.servicepackage;

import com.chamcham.backend.entity.enums.PackagePlatform;
import com.chamcham.backend.entity.enums.PackagePricingType;
import com.chamcham.backend.entity.enums.PackageType;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ServicePackageCreateRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 2000) String description,
        @NotNull PackagePlatform platform,
        @Size(max = 80) String category,
        @NotNull PackageType type,
        PackagePricingType pricingType,
        @Size(max = 1000) String barterDetails,
        @NotNull @DecimalMin("1.00") BigDecimal price,
        @Size(max = 10) String currency,
        @NotBlank @Size(max = 1000) String deliverables,
        @NotNull @Positive Integer deliveryDays,
        @Positive Integer durationDays,
        @Positive Integer revisions,
        Boolean isFeatured,
        String coverImage,
        List<String> mediaUrls,
        List<String> tags,
        Boolean isActive,
        @Valid List<ServicePackageTierRequest> tiers
) {
}



