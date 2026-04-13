package com.chamcham.backend.entity.enums;

public enum UserRole {
    CREATOR,
    BRAND,
    ADMIN;

    public boolean isCreator() {
        return this == CREATOR;
    }

    public boolean isBrand() {
        return this == BRAND;
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public static UserRole fromLegacySellerFlag(boolean isSeller) {
        return isSeller ? CREATOR : BRAND;
    }
}


