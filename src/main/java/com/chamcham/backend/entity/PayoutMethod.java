package com.chamcham.backend.entity;

import com.chamcham.backend.entity.enums.PayoutMethodType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
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
@Table(name = "payout_methods")
public class PayoutMethod {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PayoutMethodType type;

    @Column(nullable = false, length = 100)
    private String name;

    /** Raw account number – masked on read in the mapper/service. */
    @Column(name = "account_details", nullable = false, length = 300)
    private String accountDetails;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

