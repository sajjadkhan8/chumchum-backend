package com.chamcham.backend.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MessageCreateRequest(
        @NotNull UUID conversationId,
        @NotBlank String description
) {
}

