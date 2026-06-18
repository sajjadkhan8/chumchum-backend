package com.zingzing.backend.dto.quickdeal;

import jakarta.validation.constraints.NotBlank;

public record QuickDealRespondRequest(
        @NotBlank String action
) {
}

