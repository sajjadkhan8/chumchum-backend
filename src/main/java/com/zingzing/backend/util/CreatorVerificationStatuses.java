package com.zingzing.backend.util;

import com.zingzing.backend.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.Locale;
import java.util.Set;

public final class CreatorVerificationStatuses {
    public static final String UNVERIFIED = "unverified";
    public static final String PENDING = "pending";
    public static final String UNDER_REVIEW = "under_review";
    public static final String VERIFIED = "verified";
    public static final String REJECTED = "rejected";

    private static final Set<String> ALLOWED = Set.of(UNVERIFIED, PENDING, UNDER_REVIEW, VERIFIED, REJECTED);

    private CreatorVerificationStatuses() {
    }

    public static String normalize(String status) {
        String normalized = normalizeForResponse(status);
        if (!ALLOWED.contains(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid creator verification status: " + status);
        }
        return normalized;
    }

    public static String normalizeForResponse(String status) {
        if (status == null || status.isBlank()) return UNVERIFIED;
        return status.trim().toLowerCase(Locale.ROOT).replace(" ", "_").replace("-", "_");
    }
}
