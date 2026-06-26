package com.zingzing.backend.dto.campaign;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record BrandCampaignCreateRequest(
         @NotBlank @Size(max = 160) String title,
         @NotBlank @Size(max = 2000) String brief,
         @NotBlank @Size(max = 40) String offerType,
         @Min(0) Integer budgetMin,
         @Min(0) Integer budgetMax,
         @Size(max = 10) String currency,
         @Size(max = 30) String budgetType,
         @Size(max = 30) String paymentStructure,
         @Size(max = 2000) String barterProductDesc,
         @Min(0) Integer barterEstimatedValue,
         Boolean travelCostsCovered,
         @Size(max = 2000) String deliverables,
         @Size(max = 300) String contentFormats,
         @Size(max = 300) String targetPlatforms,
         @Size(max = 150) String campaignGoal,
         @Size(max = 400) String categories,
         String referenceUrls,
         @Size(max = 4000) String keyMessage,
         @Size(max = 4000) String dosAndDonts,
         @Size(max = 2000) String hashtagsMentions,
         @Size(max = 4000) String usageRights,
         @Size(max = 4000) String termsAndConditions,
         @Size(max = 4000) String expectedOutcomes,
         @Size(max = 600) String coverImageUrl,
         LocalDate deadlineDate,
         @Size(max = 30) String locationTargetingMode,
         String targetCities,
         @Size(max = 100) String targetRegion,
         @Size(max = 100) String targetCity,
         @Size(max = 100) String targetLanguage,
         @Size(max = 20) String visibility,
         // Control tab fields
         @Size(max = 50) String creatorType,
         @Size(max = 50) String followerRange,
         @Size(max = 20) String creatorGenderPreference,
         @Min(5) @DecimalMax("120") Integer minAge,
         @Min(5) @DecimalMax("120") Integer maxAge,
         @Size(max = 50) String applicationType,
         @Min(1) Integer maxApplicants,
         @Min(0) Integer minProposedPrice,
         Boolean proposalRequired,
         Boolean portfolioRequired,
         String customScreeningQuestions,
         LocalDate contentSubmissionDeadline,
         LocalDate goLiveDate,
         @Min(1) Integer campaignDuration
 ) {
 }
