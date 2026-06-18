package com.zingzing.backend.entity.enums;

public enum BrandPaymentAccessRole {
    OWNER,
    ADMIN,
    FINANCE,
    VIEWER;

    public boolean canManageFunds() {
        return this == OWNER || this == ADMIN || this == FINANCE;
    }

    public boolean canView() {
        return true;
    }
}

