package com.zingzing.backend.dto.order;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UpdateOrderStatusRequest(
        @NotBlank String status,
        String progressUpdate,
        String message
) {
}

