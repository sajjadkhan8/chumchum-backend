package com.chamcham.backend.entity;

import com.chamcham.backend.entity.enums.AffiliateCommissionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "affiliate_commissions")
public class AffiliateCommission extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "affiliate_owner_user_id", nullable = false)
    private User affiliateOwner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "earning_creator_id", nullable = false)
    private Creator earningCreator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "base_amount", nullable = false)
    private int baseAmount;

    @Column(name = "rate_basis_points", nullable = false)
    private int rateBasisPoints;

    @Column(name = "commission_amount", nullable = false)
    private int commissionAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AffiliateCommissionStatus status;
}
