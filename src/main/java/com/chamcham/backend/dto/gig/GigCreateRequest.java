package com.chamcham.backend.dto.gig;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record GigCreateRequest(
        @NotBlank String title,
        @NotBlank String description,
        @NotBlank String category,
        @NotNull @DecimalMin("1.00") BigDecimal price,
        @NotBlank String cover,
        List<String> images,
        @NotBlank String shortTitle,
        @NotBlank String shortDescription,
        @NotBlank String deliveryTime,
        @Positive int revisionNumber,
        List<String> features
) {
}

