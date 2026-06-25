package com.zingzing.backend.dto.media;

import java.util.List;
import java.util.Map;

public record MediaUploadLimitsResponse(
        long userStorageLimitMb,
        long packageStorageLimitMb,
        long campaignStorageLimitMb,
        int userUploadCountLimit,
        int packageUploadCountLimit,
        int campaignUploadCountLimit,
        Map<String, UploadRuleResponse> uploads
) {
    public record UploadRuleResponse(long maxMb, List<String> allowedTypes, String resourceType) {}
}
