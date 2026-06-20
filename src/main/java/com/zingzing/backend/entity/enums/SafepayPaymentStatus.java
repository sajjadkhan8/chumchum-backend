package com.zingzing.backend.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum SafepayPaymentStatus {
    /** Checkout URL generated; brand has not yet paid. */
    INITIATED,
    /** Webhook payment.succeeded received and wallet/order credited. */
    COMPLETED,
    /** Webhook payment.failed received. */
    FAILED,
    /** Brand navigated to cancel_url without paying. */
    CANCELLED,
    /** Session expired (1-hour TTL) before payment was attempted. */
    EXPIRED;

    @JsonCreator
    public static SafepayPaymentStatus fromJson(String value) {
        if (value == null || value.isBlank()) return null;
        return SafepayPaymentStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    @JsonValue
    public String toJson() {
        return name().toLowerCase(Locale.ROOT);
    }
}
