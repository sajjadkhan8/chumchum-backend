package com.chamcham.backend.entity.enums;

public enum UserRole {
    CREATOR,
    BRAND,
    PLATFORM_ADMIN;

    public boolean isCreator() {
        return this == CREATOR;
    }

    public boolean isBrand() {
        return this == BRAND;
    }

    public boolean isAdmin() {
        return this == PLATFORM_ADMIN;
    }
}


