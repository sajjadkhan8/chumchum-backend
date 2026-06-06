package com.chamcham.backend.dto.servicepackage;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ServicePackageTierRequest(
        @NotBlank @Size(max = 50) String name,
        @NotNull @Min(1) Integer price,  // PKR amount in integer
        @Size(max = 1000) String description,
        @NotNull @Size(min = 1) List<@NotBlank @Size(max = 200) String> deliverables,
        @Positive Integer deliveryDays,
        @Positive Integer revisions,
        @Min(0) Integer position,  // Order of display
        Boolean isPrimary  // V1: markup one tier as primary
) {
}

