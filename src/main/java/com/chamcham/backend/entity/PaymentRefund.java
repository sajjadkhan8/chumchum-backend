package com.chamcham.backend.entity;

import com.chamcham.backend.entity.enums.TransactionStatus;
import com.chamcham.backend.entity.enums.TransactionStatusConverter;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "payment_refunds")
public class PaymentRefund {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dispute_id", nullable = false, unique = true)
    private DisputeCase dispute;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "executed_by_admin_id", nullable = false)
    private User executedByAdmin;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false, length = 500)
    private String reason;

    @Convert(converter = TransactionStatusConverter.class)
    @Column(nullable = false, length = 30)
    private TransactionStatus status;

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(name = "provider_refund_id", nullable = false, unique = true, length = 100)
    private String providerRefundId;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "creator_clawback_amount", nullable = false)
    @Builder.Default
    private int creatorClawbackAmount = 0;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
