package com.chamcham.backend.dto.creator;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ContentPreviewResponse(
        UUID id,
        String type,
        String thumbnailUrl,
        String mediaUrl,
        String platform,
        Integer views,
        Integer likes
) {
}
