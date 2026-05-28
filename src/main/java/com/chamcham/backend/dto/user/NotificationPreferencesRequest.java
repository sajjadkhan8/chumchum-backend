package com.chamcham.backend.dto.user;

public record NotificationPreferencesRequest(
        boolean newOrders,
        boolean messages,
        boolean reviews,
        boolean marketing,
        boolean weeklyDigest,
        boolean pushNotifications,
        boolean emailNotifications,
        boolean smsNotifications
) {
}

