package com.chamcham.backend.entity;

import com.chamcham.backend.entity.enums.BrandPlanTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "brands", schema = "core")
@PrimaryKeyJoinColumn(name = "id")
public class Brand extends User {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(length = 255)
    private String website;

    @Column(length = 100)
    private String industry;

    @Column(length = 1000)
    private String description;

    @Column(name = "monthly_budget")
    private Integer monthlyBudget;

    @Column(name = "preferred_creator_categories", length = 500)
    private String preferredCreatorCategories;

    @Column(name = "target_cities", length = 500)
    private String targetCities;

    @Column(name = "target_platforms", length = 500)
    private String targetPlatforms;

    @Column(name = "campaign_budget_range", length = 150)
    private String campaignBudgetRange;

    @Column(name = "business_verification_status", length = 50)
    private String businessVerificationStatus;

    @Column(name = "verification_contact_email", length = 255)
    private String verificationContactEmail;

    @Column(name = "verification_phone_number", length = 50)
    private String verificationPhoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_tier", nullable = false, length = 20)
    private BrandPlanTier planTier = BrandPlanTier.STARTER;

    public String getDisplayName() {
        return name != null && !name.isBlank() ? name : super.getName();
    }
}
