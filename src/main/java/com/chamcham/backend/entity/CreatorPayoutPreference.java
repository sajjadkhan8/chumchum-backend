package com.chamcham.backend.entity;

import com.chamcham.backend.entity.enums.CreatorPayoutSchedule;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.EntityListeners;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "creator_payout_preferences")
public class CreatorPayoutPreference {

    @Id
    @Column(name = "creator_id", nullable = false)
    private UUID creatorId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id")
    private Creator creator;

    @Column(name = "auto_withdraw_enabled", nullable = false)
    @Builder.Default
    private boolean autoWithdrawEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "payout_schedule", nullable = false, length = 20)
    @Builder.Default
    private CreatorPayoutSchedule payoutSchedule = CreatorPayoutSchedule.MANUAL;

    @Column(name = "minimum_payout_amount", nullable = false)
    @Builder.Default
    private int minimumPayoutAmount = 5000;

    @Column(name = "account_holder_name", nullable = false, length = 120)
    @Builder.Default
    private String accountHolderName = "";

    @Column(name = "ntn_number", nullable = false, length = 30)
    @Builder.Default
    private String ntnNumber = "";

    @Column(name = "cnic_last4", nullable = false, length = 4)
    @Builder.Default
    private String cnicLast4 = "";

    @Column(name = "earnings_notifications_enabled", nullable = false)
    @Builder.Default
    private boolean earningsNotificationsEnabled = true;

    @Column(name = "weekly_digest_enabled", nullable = false)
    @Builder.Default
    private boolean weeklyDigestEnabled = false;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

