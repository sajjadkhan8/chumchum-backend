package com.zingzing.backend.dto.brand;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Size;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record BrandUpdateRequest(
        @Size(max = 150) String companyName,
        @Size(max = 255) String website,
        @Size(max = 100) String industry,
        @Size(max = 1000) String description,
        @Size(max = 500) String logoUrl,
        Integer monthlyBudget,
        @Size(max = 500) String preferredCreatorCategories,
        @Size(max = 500) String targetCities,
        @Size(max = 500) String targetPlatforms,
        @Size(max = 150) String campaignBudgetRange,
        @Size(max = 50) String businessVerificationStatus,
        @Size(max = 255) String verificationContactEmail,
        @Size(max = 50) String verificationPhoneNumber
) {
}
