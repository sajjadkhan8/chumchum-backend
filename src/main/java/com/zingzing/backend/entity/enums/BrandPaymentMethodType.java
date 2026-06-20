package com.zingzing.backend.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum BrandPaymentMethodType {
    CARD,
    BANK_TRANSFER,
    JAZZCASH,
    EASYPAISA,
    SADAPAY,
    NAYAPAY;

    @JsonCreator
    public static BrandPaymentMethodType fromJson(String value) {
        if (value == null || value.isBlank()) return null;
        return BrandPaymentMethodType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    @JsonValue
    public String toJson() {
        return name().toLowerCase(Locale.ROOT);
    }
}
