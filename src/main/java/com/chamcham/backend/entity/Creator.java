package com.chamcham.backend.entity;

import com.chamcham.backend.entity.enums.AvailabilityStatus;
import com.chamcham.backend.entity.enums.CreatorBadgeLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "creators", schema = "core")
@PrimaryKeyJoinColumn(name = "id")
public class Creator extends User {


    @Column(length = 1000)
    private String bio;

    @Column(length = 100)
    private String category;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(length = 300)
    private String website;

    @Column(length = 100)
    private String niche;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", length = 30)
    private AvailabilityStatus availabilityStatus;

    @Column(name = "is_filer", nullable = false)
    private boolean isFiler = false;

    @Column(name = "response_time", length = 50)
    private String responseTime;

    @Column(name = "min_price")
    private Integer minPrice;

    @Column(name = "max_price")
    private Integer maxPrice;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "badge_level", nullable = false, length = 30)
    private CreatorBadgeLevel badgeLevel = CreatorBadgeLevel.NONE;

    @Column(name = "is_trending", nullable = false)
    private boolean isTrending = false;

    @Column(name = "is_fast_responder", nullable = false)
    private boolean isFastResponder = false;

    @Column(name = "completed_deals", nullable = false)
    private int completedDeals = 0;

    @Column(name = "accepts_barter", nullable = false)
    private boolean acceptsBarter = true;

    @Column(name = "accepts_hybrid_deals", nullable = false)
    private boolean acceptsHybridDeals = true;

    @Column(name = "minimum_budget")
    private Integer minimumBudget;

    @Column(name = "preferred_industries", columnDefinition = "text")
    private String preferredIndustries;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "languages", columnDefinition = "jsonb")
    private List<String> languages = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "categories", columnDefinition = "jsonb")
    private List<String> categories = new ArrayList<>();

    // legacy social URL fields (kept for backward compat, new data uses social_accounts table)
    @Column(name = "tiktok_url", length = 255)
    private String tiktokUrl;

    @Column(name = "instagram_url", length = 255)
    private String instagramUrl;

    @Column(name = "youtube_url", length = 255)
    private String youtubeUrl;

    @Column(name = "facebook_url", length = 255)
    private String facebookUrl;

    @Column(nullable = false)
    private int followers = 0;

    @Column(name = "avg_views", nullable = false)
    private int avgViews = 0;

    @Column(name = "engagement_rate", precision = 5, scale = 2)
    private BigDecimal engagementRate;

    @Column(precision = 3, scale = 2, nullable = false)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "total_reviews", nullable = false)
    private int totalReviews = 0;

    @OneToMany(mappedBy = "creator", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServicePackage> packages = new ArrayList<>();

    @OneToMany(mappedBy = "creator", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SocialAccount> socialAccounts = new ArrayList<>();

    @OneToMany(mappedBy = "creator", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContentPreview> contentPreviews = new ArrayList<>();
}
