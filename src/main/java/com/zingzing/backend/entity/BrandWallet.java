package com.zingzing.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "brand_wallets")
public class BrandWallet {

    @Id
    @Column(name = "brand_id", nullable = false)
    private UUID brandId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Column(name = "wallet_balance", nullable = false)
    @Builder.Default
    private int walletBalance = 0;

    @Column(name = "monthly_spend", nullable = false)
    @Builder.Default
    private int monthlySpend = 0;

    @Column(name = "pending_escrow", nullable = false)
    @Builder.Default
    private int pendingEscrow = 0;

    @Column(name = "processing_payouts", nullable = false)
    @Builder.Default
    private int processingPayouts = 0;

    @Column(name = "next_invoice_date")
    private Instant nextInvoiceDate;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

