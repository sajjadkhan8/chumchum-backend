package com.zingzing.backend.repository;

import com.zingzing.backend.entity.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID> {

    @Query("""
            select b from Brand b
            where (cast(:search as string) is null
                   or lower(b.name) like concat('%', lower(cast(:search as string)), '%')
                   or lower(b.email) like concat('%', lower(cast(:search as string)), '%')
                   or lower(b.username) like concat('%', lower(cast(:search as string)), '%'))
              and (
                :verificationStatus is null
                or (:verificationStatus = 'pending' and (b.businessVerificationStatus is null or lower(b.businessVerificationStatus) = 'pending'))
                or lower(b.businessVerificationStatus) = :verificationStatus
              )
            order by b.createdAt desc
            """)
    Page<Brand> searchForAdmin(@Param("search") String search,
                               @Param("verificationStatus") String verificationStatus,
                               Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO core.brands (
                id, name, logo_url, website, industry, description,
                monthly_budget, preferred_creator_categories, target_cities,
                target_platforms, campaign_budget_range,
                business_verification_status, verification_contact_email,
                verification_phone_number,
                company_size, contact_name, contact_email, contact_phone
            ) VALUES (
                :id, :name, :logoUrl, :website, :industry, :description,
                :monthlyBudget, :preferredCreatorCategories, :targetCities,
                :targetPlatforms, :campaignBudgetRange,
                :businessVerificationStatus, :verificationContactEmail,
                :verificationPhoneNumber,
                :companySize, :contactName, :contactEmail, :contactPhone
            )
            """, nativeQuery = true)
    int insertProfile(
            @Param("id") UUID id,
            @Param("name") String name,
            @Param("logoUrl") String logoUrl,
            @Param("website") String website,
            @Param("industry") String industry,
            @Param("description") String description,
            @Param("monthlyBudget") Integer monthlyBudget,
            @Param("preferredCreatorCategories") String preferredCreatorCategories,
            @Param("targetCities") String targetCities,
            @Param("targetPlatforms") String targetPlatforms,
            @Param("campaignBudgetRange") String campaignBudgetRange,
            @Param("businessVerificationStatus") String businessVerificationStatus,
            @Param("verificationContactEmail") String verificationContactEmail,
            @Param("verificationPhoneNumber") String verificationPhoneNumber,
            @Param("companySize") String companySize,
            @Param("contactName") String contactName,
            @Param("contactEmail") String contactEmail,
            @Param("contactPhone") String contactPhone
    );

    @Query("select count(b) from Brand b where lower(b.businessVerificationStatus) = 'pending'")
    long countPendingVerifications();
}
