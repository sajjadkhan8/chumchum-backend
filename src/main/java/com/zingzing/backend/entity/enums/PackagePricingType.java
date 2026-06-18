package com.zingzing.backend.entity.enums;

/**
 * Legacy alias – use {@link DealType} for new code.
 * Kept only so existing references in ServicePackageCreateRequest compile.
 */
public enum PackagePricingType {
    PAID, BARTER, HYBRID;

    public DealType toDealType() {
        return DealType.valueOf(this.name());
    }
}
