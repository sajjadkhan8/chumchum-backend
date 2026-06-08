package com.chamcham.backend.dto.offer;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BrandOfferUpdateRequest(
        @Size(max = 160) String title,
        @Size(max = 2000) String brief,
        @Size(max = 40) String offerType,
        @Min(0) Integer budgetMin,
        @Min(0) Integer budgetMax,
        @Size(max = 10) String currency,
        @Size(max = 2000) String deliverables,
        @Size(max = 2000) String requirements,
        LocalDate deadlineDate,
        @Size(max = 100) String targetCity,
        @Size(max = 100) String targetLanguage,
        @Min(0) Integer minFollowers,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal minEngagementRate
) {
}

