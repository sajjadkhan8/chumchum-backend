package com.chamcham.backend.entity;

import com.chamcham.backend.entity.enums.PackagePlatform;
import com.chamcham.backend.entity.enums.PackagePricingType;
import com.chamcham.backend.entity.enums.PackageType;
import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
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

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PackagePlatform platform;

    @Column(length = 80)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PackageType type;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_type", nullable = false, length = 30)
    private PackagePricingType pricingType = PackagePricingType.PAID;

    @Column(name = "barter_details", length = 1000)
    private String barterDetails;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Builder.Default
    @Column(length = 10)
    private String currency = "PKR";

    @Column(length = 1000, nullable = false)
    private String deliverables;

    @Column(nullable = false)
    private int deliveryDays;

    private Integer durationDays;

    @Builder.Default
    @Column(nullable = false)
    private int revisions = 1;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(name = "is_featured", nullable = false)
    private boolean featured = false;

    @Column(length = 500)
    private String coverImage;

    @Column(name = "media_urls", columnDefinition = "text[]")
    private String[] mediaUrls;

    @Column(name = "tags", columnDefinition = "text[]")
    private String[] tags;

    @OneToMany(mappedBy = "servicePackage", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PackageTier> tiers = new ArrayList<>();
}



