package com.zingzing.backend.dto.media;

import java.util.UUID;

public record MediaUploadResponse(
        UUID mediaId,
        String url,
        String secureUrl,
        String thumbnailUrl,
        String publicId,
        String resourceType,
        String format,
        Long bytes,
        Integer width,
        Integer height,
        Double duration
) {}
