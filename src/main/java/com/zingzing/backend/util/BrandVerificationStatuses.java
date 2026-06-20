package com.zingzing.backend.util;

import com.zingzing.backend.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.Locale;
import java.util.Set;

public final class BrandVerificationStatuses {

    public static final String UNVERIFIED = "unverified";
    public static final String PENDING = "pending";
    public static final String UNDER_REVIEW = "under_review";
    public static final String VERIFIED = "verified";
    public static final String REJECTED = "rejected";

    private static final Set<String> VALID = Set.of(UNVERIFIED, PENDING, UNDER_REVIEW, VERIFIED, REJECTED);

    private BrandVerificationStatuses() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) return UNVERIFIED;
        String value = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if ("underreview".equals(value)) return UNDER_REVIEW;
        if (VALID.contains(value)) return value;
        throw new ApiException(HttpStatus.BAD_REQUEST,
                "verification status must be unverified, pending, under_review, verified, or rejected");
    }

    public static String normalizeNullable(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return normalize(raw);
    }

    public static String normalizeForResponse(String raw) {
        try {
            return normalize(raw);
        } catch (ApiException ignored) {
            return UNVERIFIED;
        }
    }
}
