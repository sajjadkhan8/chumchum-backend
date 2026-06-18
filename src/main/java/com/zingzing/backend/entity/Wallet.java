package com.zingzing.backend.entity;

import jakarta.persistence.*;
import lombok.*;
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
@Table(name = "wallets")
public class Wallet {

    @Id
    @Column(name = "creator_id", nullable = false)
    private UUID creatorId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id")
    private Creator creator;

    @Column(name = "total_earned", nullable = false)
    @Builder.Default
    private int totalEarned = 0;

    @Column(name = "available_balance", nullable = false)
    @Builder.Default
    private int availableBalance = 0;

    @Column(name = "pending_balance", nullable = false)
    @Builder.Default
    private int pendingBalance = 0;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

