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
        @Pattern(regexp = "^$|[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "verificationContactEmail must be a valid email")
        @Size(max = 255) String verificationContactEmail,
        @Pattern(regexp = "^$|\\+?[0-9\\s().-]{7,30}", message = "verificationPhoneNumber must be a valid phone number")
        @Size(max = 50) String verificationPhoneNumber,
        @Size(max = 80)  String city,
        @Size(max = 50)  String companySize,
        @Size(max = 100) String contactName,
        @Pattern(regexp = "^$|[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "contactEmail must be a valid email")
        @Size(max = 120) String contactEmail,
        @Pattern(regexp = "^$|\\+?[0-9\\s().-]{7,30}", message = "contactPhone must be a valid phone number")
        @Size(max = 30)  String contactPhone
) {
}
