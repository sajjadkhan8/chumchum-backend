package com.zingzing.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "new_orders", nullable = false)
    @Builder.Default
    private boolean newOrders = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean messages = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean reviews = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean marketing = false;

    @Column(name = "weekly_digest", nullable = false)
    @Builder.Default
    private boolean weeklyDigest = true;

    @Column(name = "push_notifications", nullable = false)
    @Builder.Default
    private boolean pushNotifications = true;

    @Column(name = "email_notifications", nullable = false)
    @Builder.Default
    private boolean emailNotifications = true;

    @Column(name = "sms_notifications", nullable = false)
    @Builder.Default
    private boolean smsNotifications = false;
}
