package com.chamcham.backend.entity;

import com.chamcham.backend.entity.enums.DealType;
import com.chamcham.backend.entity.enums.OfferStatus;
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
@Table(name = "quick_deal_offers")
public class QuickDealOffer {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private Message messageEntity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(name = "deal_type", nullable = false, length = 30)
    private DealType dealType;

    @Column
    private Integer amount;

    @Column(name = "barter_details", columnDefinition = "text")
    private String barterDetails;

    @Column(name = "barter_category", length = 100)
    private String barterCategory;

    @Column(name = "estimated_barter_value")
    private Integer estimatedBarterValue;

    @Column(name = "creator_expectation", columnDefinition = "text")
    private String creatorExpectation;

    @Column(name = "message", nullable = false, columnDefinition = "text")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OfferStatus status = OfferStatus.PENDING;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

