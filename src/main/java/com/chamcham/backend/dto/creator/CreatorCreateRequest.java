package com.chamcham.backend.dto.creator;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreatorCreateRequest(
        @NotNull UUID userId,
        @Size(max = 1000) String bio,
        @Size(max = 100) String category,
        @Size(max = 255) String tiktokUrl,
        @Size(max = 255) String instagramUrl,
        @Size(max = 255) String youtubeUrl
) {
}

