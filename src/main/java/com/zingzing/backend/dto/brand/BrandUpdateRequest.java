package com.zingzing.backend.dto.brand;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record BrandUpdateRequest(
        @Size(max = 150) String companyName,
        @Pattern(regexp = "^$|https?://.+", message = "website must start with http:// or https://")
        @Size(max = 255) String website,
        @Size(max = 50) String category,
        @Size(max = 1000) String description,
        @Size(max = 500) String logoUrl,
        @PositiveOrZero
        Integer monthlyBudget,
        @Size(max = 500) String preferredCreatorCategories,
        @Size(max = 80)  String city,
        @Size(max = 50)  String companySize,
        @Size(max = 100) String contactName
) {
}
