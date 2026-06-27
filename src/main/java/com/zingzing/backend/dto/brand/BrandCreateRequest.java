package com.zingzing.backend.dto.brand;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record BrandCreateRequest(
        UUID userId,
        @NotBlank @Size(max = 150) String companyName,
        @Size(max = 255) String website,
        @Size(max = 50) String category,
        @Size(max = 1000) String description
) {
}
