package com.chamcham.backend.entity;

import com.chamcham.backend.entity.enums.BrandOfferStatus;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "brand_offers", schema = "core")
public class BrandOffer extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 2000)
    private String brief;

    @Column(name = "offer_type", nullable = false, length = 40)
    private String offerType;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "PKR";

    @Column(name = "budget_min")
    private Integer budgetMin;

    @Column(name = "budget_max")
    private Integer budgetMax;

    @Column(name = "deliverables", columnDefinition = "text")
    private String deliverables;

    @Column(name = "content_formats", length = 300)
    private String contentFormats;

    @Column(name = "target_platforms", length = 300)
    private String targetPlatforms;

    @Column(name = "categories", length = 400)
    private String categories;

    @Column(name = "niches", length = 400)
    private String niches;

    @Column(name = "tags", length = 400)
    private String tags;

    @Column(name = "requirements", columnDefinition = "text")
    private String requirements;

    @Column(name = "reference_urls", columnDefinition = "text")
    private String referenceUrls;

    @Column(name = "cover_image_url", length = 600)
    private String coverImageUrl;

    @Column(name = "deadline_date")
    private LocalDate deadlineDate;

    @Column(name = "target_city", length = 100)
    private String targetCity;

    @Column(name = "target_language", length = 100)
    private String targetLanguage;

    @Column(name = "min_followers")
    private Integer minFollowers;

    @Column(name = "min_engagement_rate", precision = 5, scale = 2)
    private java.math.BigDecimal minEngagementRate;

    @Column(name = "preferred_delivery_days")
    private Integer preferredDeliveryDays;

    @Column(name = "slots")
    private Integer slots;

    @Column(name = "visibility", nullable = false, length = 20)
    @Builder.Default
    private String visibility = "public";

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BrandOfferStatus status = BrandOfferStatus.DRAFT;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "closed_at")
    private Instant closedAt;
}

