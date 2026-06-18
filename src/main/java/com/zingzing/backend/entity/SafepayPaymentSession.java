package com.zingzing.backend.entity;

import com.zingzing.backend.entity.enums.SafepayPaymentStatus;
import com.zingzing.backend.entity.enums.SafepayPaymentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks a Safepay Express Checkout session from initiation through completion.
 *
 * One row is created when a brand initiates a checkout, and updated when
 * Safepay fires a webhook confirming success or failure.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "safepay_payment_sessions",
    schema = "core",
    uniqueConstraints = @UniqueConstraint(name = "uk_safepay_tracker", columnNames = "tracker_token")
)
public class SafepayPaymentSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /** Safepay tracker token ("track_..."). Unique per checkout attempt. */
    @Column(name = "tracker_token", nullable = false, length = 120)
    private String trackerToken;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    /** Set for ORDER_PAYMENT sessions; null for WALLET_TOPUP. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    /** Amount in whole PKR (our internal representation, not paisa). */
    @Column(name = "amount_pkr", nullable = false)
    private int amountPkr;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 30)
    private SafepayPaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private SafepayPaymentStatus status = SafepayPaymentStatus.INITIATED;

    /** Safepay's internal charge/payment reference received via webhook. */
    @Column(name = "safepay_payment_ref", length = 200)
    private String safepayPaymentRef;

    /** Failure description received via webhook payment.failed event. */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /** Raw JSON snapshot of the Safepay tracker state for audit/debugging. */
    @Column(name = "metadata_json", columnDefinition = "text")
    private String metadataJson;

    /** Sessions expire after 1 hour, matching Safepay's auth token TTL. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
