package com.chamcham.backend.dto.creator;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreatorUpdateRequest(
        @Size(max = 1000) String bio,
        @Size(max = 100) String category,
        @Size(max = 255) String tiktokUrl,
        @Size(max = 255) String instagramUrl,
        @Size(max = 255) String youtubeUrl,
        @Min(0) Integer followers,
        @Min(0) Integer avgViews,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal engagementRate,
        @DecimalMin("0.00") @DecimalMax("5.00") BigDecimal rating,
        @Min(0) Integer totalReviews
) {
}

