package com.chamcham.backend.entity;

import com.chamcham.backend.entity.enums.BrandOfferReactionStatus;
import com.chamcham.backend.entity.enums.BrandOfferReactionType;
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
@Table(name = "brand_offer_reactions", schema = "core")
public class BrandOfferReaction extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    private BrandOffer offer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false, length = 30)
    private BrandOfferReactionType reactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private BrandOfferReactionStatus status = BrandOfferReactionStatus.SUBMITTED;

    @Column(columnDefinition = "text")
    private String message;

    @Column(name = "proposed_price")
    private Integer proposedPrice;

    @Column(name = "proposed_currency", length = 10)
    @Builder.Default
    private String proposedCurrency = "PKR";

    @Column(name = "proposed_delivery_days")
    private Integer proposedDeliveryDays;

    @Column(name = "brand_note", columnDefinition = "text")
    private String brandNote;

    @Column(name = "creator_note", columnDefinition = "text")
    private String creatorNote;
}

