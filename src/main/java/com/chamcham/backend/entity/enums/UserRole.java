package com.chamcham.backend.entity.enums;

public enum UserRole {
    CREATOR,
    BRAND,
    PLATFORM_ADMIN,
    SUPPORT,
    FINANCE_OPS;

    public boolean isCreator() { return this == CREATOR; }
    public boolean isBrand() { return this == BRAND; }
    public boolean isPlatformAdmin() { return this == PLATFORM_ADMIN; }
    public boolean isSupport() { return this == SUPPORT; }
    public boolean isFinanceOps() { return this == FINANCE_OPS; }

    /** True for any admin-tier role — governs refresh TTL and broad admin access. */
    public boolean isAdmin() {
        return this == PLATFORM_ADMIN || this == SUPPORT || this == FINANCE_OPS;
    }

    /** True only for roles that can execute financial operations (refunds, withdrawals). */
    public boolean canProcessPayments() {
        return this == PLATFORM_ADMIN || this == FINANCE_OPS;
    }

    /** True only for roles that can moderate user accounts (ban, suspend, verify). */
    public boolean canModerateUsers() {
        return this == PLATFORM_ADMIN || this == SUPPORT;
    }
}
