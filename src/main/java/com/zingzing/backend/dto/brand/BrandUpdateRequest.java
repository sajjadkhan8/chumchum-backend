package com.zingzing.backend.dto.brand;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Size;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record BrandUpdateRequest(
        @Size(max = 150) String companyName,
        @Size(max = 255) String website,
        @Size(max = 50) String category,
        @Size(max = 1000) String description,
        @Size(max = 500) String logoUrl,
        Integer monthlyBudget,
        @Size(max = 500) String preferredCreatorCategories,
        @Size(max = 255) String verificationContactEmail,
        @Size(max = 50) String verificationPhoneNumber,
        @Size(max = 80)  String city,
        @Size(max = 50)  String companySize,
        @Size(max = 100) String contactName,
        @Size(max = 120) String contactEmail,
        @Size(max = 30)  String contactPhone
) {
}
