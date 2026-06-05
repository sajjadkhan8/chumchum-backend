package com.chamcham.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
}
