package com.chamcham.backend.dto.brand;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Size;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record BrandUpdateRequest(
        @Size(max = 150) String companyName,
        @Size(max = 255) String website,
        @Size(max = 100) String industry,
        @Size(max = 1000) String description
) {
}

