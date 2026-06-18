package com.zingzing.backend.entity;

import com.zingzing.backend.entity.enums.BrandCampaignStatus;
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
@Table(name = "brand_campaigns", schema = "core")
public class BrandCampaign extends BaseEntity {

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

    @Column(name = "campaign_goal", length = 150)
    private String campaignGoal;

    @Column(name = "categories", length = 400)
    private String categories;

    @Column(name = "niches", length = 400)
    private String niches;


    @Column(name = "reference_urls", columnDefinition = "text")
    private String referenceUrls;

    @Column(name = "key_message", columnDefinition = "text")
    private String keyMessage;

    @Column(name = "dos_and_donts", columnDefinition = "text")
    private String dosAndDonts;

    @Column(name = "hashtags_mentions", columnDefinition = "text")
    private String hashtagsMentions;

    @Column(name = "usage_rights", columnDefinition = "text")
    private String usageRights;

    @Column(name = "terms_and_conditions", columnDefinition = "text")
    private String termsAndConditions;

    @Column(name = "expected_outcomes", columnDefinition = "text")
    private String expectedOutcomes;

    @Column(name = "cover_image_url", length = 600)
    private String coverImageUrl;

    @Column(name = "deadline_date")
    private LocalDate deadlineDate;

    @Column(name = "location_targeting_mode", length = 30)
    private String locationTargetingMode;

    @Column(name = "target_cities", columnDefinition = "text")
    private String targetCities;

    @Column(name = "target_region", length = 100)
    private String targetRegion;

    @Column(name = "target_city", length = 100)
    private String targetCity;

    @Column(name = "target_language", length = 100)
    private String targetLanguage;


    @Column(name = "budget_type", length = 30)
    private String budgetType;

    @Column(name = "payment_structure", length = 30)
    private String paymentStructure;

    @Column(name = "barter_product_desc", columnDefinition = "text")
    private String barterProductDesc;

    @Column(name = "barter_estimated_value")
    private Integer barterEstimatedValue;

     @Column(name = "travel_costs_covered", nullable = false)
     @Builder.Default
     private Boolean travelCostsCovered = false;

     @Column(name = "visibility", nullable = false, length = 20)
     @Builder.Default
     private String visibility = "public";

     // Control tab fields
     @Column(name = "creator_type", length = 50)
     private String creatorType;

     @Column(name = "follower_range", length = 50)
     private String followerRange;

     @Column(name = "creator_gender_preference", length = 20)
     private String creatorGenderPreference;

     @Column(name = "min_age")
     private Integer minAge;

     @Column(name = "max_age")
     private Integer maxAge;

     @Column(name = "application_type", length = 50)
     private String applicationType;

     @Column(name = "max_applicants")
     private Integer maxApplicants;

     @Column(name = "min_proposed_price")
     private Integer minProposedPrice;

     @Column(name = "proposal_required", nullable = false)
     @Builder.Default
     private Boolean proposalRequired = false;

     @Column(name = "portfolio_required", nullable = false)
     @Builder.Default
     private Boolean portfolioRequired = false;

     @Column(name = "custom_screening_questions", columnDefinition = "text")
     private String customScreeningQuestions;

     @Column(name = "content_submission_deadline")
     private LocalDate contentSubmissionDeadline;

     @Column(name = "go_live_date")
     private LocalDate goLiveDate;

     @Column(name = "campaign_duration")
     private Integer campaignDuration;

     @Column(nullable = false, length = 30)
     @Enumerated(EnumType.STRING)
     @Builder.Default
     private BrandCampaignStatus status = BrandCampaignStatus.DRAFT;

     @Column(name = "published_at")
     private Instant publishedAt;

     @Column(name = "closed_at")
     private Instant closedAt;
 }
