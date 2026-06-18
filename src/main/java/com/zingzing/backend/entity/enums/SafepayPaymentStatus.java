package com.zingzing.backend.entity.enums;

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
    EXPIRED
}
