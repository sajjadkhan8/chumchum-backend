package com.zingzing.backend.dto.media;

public record StoredMedia(
        String url,
        String secureUrl,
        String thumbnailUrl,
        String publicId,
        String assetId,
        String resourceType,
        String format,
        Long bytes,
        Integer width,
        Integer height,
        Double duration
) {}
