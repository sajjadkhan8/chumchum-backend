package com.zingzing.backend.entity.enums;

public enum SafepayPaymentType {
    /** Brand is adding funds to their platform wallet. */
    WALLET_TOPUP,
    /** Brand is paying directly for a specific order. */
    ORDER_PAYMENT
}
