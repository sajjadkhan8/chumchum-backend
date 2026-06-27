package com.zingzing.backend.entity;

import com.zingzing.backend.entity.enums.BrandPlanTier;
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

import java.math.BigDecimal;

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

    @Column(length = 50)
    private String category;

    @Column(length = 1000)
    private String description;

    @Column(name = "monthly_budget")
    private Integer monthlyBudget;

    @Column(name = "preferred_creator_categories", length = 500)
    private String preferredCreatorCategories;

    @Column(name = "business_verification_status", length = 50)
    private String businessVerificationStatus;

    @Column(name = "verification_contact_email", length = 255)
    private String verificationContactEmail;

    @Column(name = "verification_phone_number", length = 50)
    private String verificationPhoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_tier", nullable = false, length = 20)
    private BrandPlanTier planTier = BrandPlanTier.STARTER;

    @Column(name = "brand_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal brandRating = BigDecimal.ZERO;

    @Column(name = "brand_total_reviews", nullable = false)
    private int brandTotalReviews = 0;

    @Column(name = "company_size", length = 50)
    private String companySize;

    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(name = "contact_email", length = 120)
    private String contactEmail;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    public String getDisplayName() {
        return name != null && !name.isBlank() ? name : super.getName();
    }
}
