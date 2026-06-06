package com.chamcham.backend.entity;

import com.chamcham.backend.entity.enums.DealType;
import com.chamcham.backend.entity.enums.PackagePlatform;
import com.chamcham.backend.entity.enums.PackageStatus;
import com.chamcham.backend.entity.enums.PackageType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "packages", schema = "core")
public class ServicePackage extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;

    // legacy name field (kept for DB compat)
    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "short_description", length = 300)
    private String shortDescription;

    @Column(name = "full_description", columnDefinition = "text")
    private String fullDescription;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PackagePlatform platform;

    @Column(length = 80)
    private String category;

    // legacy type (ONE_TIME/SUBSCRIPTION) – kept for backward compat
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PackageType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "deal_type", length = 30)
    private DealType dealType;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private PackageStatus status = PackageStatus.DRAFT;

    @Column(length = 30)
    @Builder.Default
    private String visibility = "public";

    // SAR amount in integer (not BigDecimal) per spec
    @Column
    private Integer price;

    @Column(name = "barter_details", length = 1000)
    private String barterDetails;

    @Column(name = "barter_description", columnDefinition = "text")
    private String barterDescription;

    @Column(name = "barter_category", length = 100)
    private String barterCategory;

    @Column(name = "estimated_barter_value")
    private Integer estimatedBarterValue;

    @Column(name = "hybrid_cash_amount")
    private Integer hybridCashAmount;

    @Column(name = "hybrid_barter_value")
    private Integer hybridBarterValue;

    @Column(name = "creator_expectations", columnDefinition = "text")
    private String creatorExpectations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "deliverables", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> deliverables = new ArrayList<>();

    @Column(name = "delivery_days", nullable = false)
    private int deliveryDays;

    @Builder.Default
    @Column(nullable = false)
    private int revisions = 1;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    // legacy cover_image column retained to avoid schema conflict until removed
    @Column(name = "cover_image", length = 500)
    private String coverImage;

    @Builder.Default
    @Column(name = "is_popular", nullable = false)
    private boolean isPopular = false;

    @Builder.Default
    @Column(name = "orders_completed", nullable = false)
    private int ordersCompleted = 0;

    @Column(name = "response_time", length = 50)
    private String responseTime;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(name = "is_featured", nullable = false)
    private boolean featured = false;

    @Column(name = "media_urls", columnDefinition = "text[]")
    private String[] mediaUrls;

    @Column(length = 10)
    @Builder.Default
    private String currency = "PKR";  // V1: PKR only (mono-currency)

    @OneToMany(mappedBy = "servicePackage", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PackageTier> tiers = new ArrayList<>();

    @OneToOne(mappedBy = "servicePackage", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private PackageAnalytics analytics;
}
