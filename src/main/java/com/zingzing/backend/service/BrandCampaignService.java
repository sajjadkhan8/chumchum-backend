package com.zingzing.backend.service;

import com.zingzing.backend.dto.campaign.BrandCampaignCreateRequest;
import com.zingzing.backend.dto.campaign.BrandCampaignReactionActionRequest;
import com.zingzing.backend.dto.campaign.BrandCampaignReactionCreateRequest;
import com.zingzing.backend.dto.campaign.BrandCampaignReactionResponse;
import com.zingzing.backend.dto.campaign.BrandCampaignReactionUpdateRequest;
import com.zingzing.backend.dto.campaign.BrandCampaignResponse;
import com.zingzing.backend.dto.campaign.BrandCampaignStatusUpdateRequest;
import com.zingzing.backend.dto.campaign.BrandCampaignUpdateRequest;
import com.zingzing.backend.entity.Brand;
import com.zingzing.backend.entity.BrandCampaign;
import com.zingzing.backend.entity.BrandCampaignReaction;
import com.zingzing.backend.entity.CampaignAlertRule;
import com.zingzing.backend.entity.Creator;
import com.zingzing.backend.entity.ServicePackage;
import com.zingzing.backend.entity.enums.BrandCampaignReactionStatus;
import com.zingzing.backend.entity.enums.BrandCampaignReactionType;
import com.zingzing.backend.entity.enums.BrandCampaignStatus;
import com.zingzing.backend.entity.enums.BrandPlanTier;
import com.zingzing.backend.entity.enums.DealType;
import com.zingzing.backend.entity.enums.PackageCategory;
import com.zingzing.backend.entity.enums.PackagePlatform;
import com.zingzing.backend.entity.enums.PackageStatus;
import com.zingzing.backend.entity.enums.UserRole;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.repository.BrandCampaignReactionRepository;
import com.zingzing.backend.repository.BrandCampaignRepository;
import com.zingzing.backend.repository.CampaignAlertRuleRepository;
import com.zingzing.backend.repository.BrandRepository;
import com.zingzing.backend.repository.CreatorRepository;
import com.zingzing.backend.repository.OrderRepository;
import com.zingzing.backend.repository.ServicePackageRepository;
import com.zingzing.backend.util.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class BrandCampaignService {

    private static final int MAX_PAGE_SIZE = 50;

    private final BrandCampaignRepository brandCampaignRepository;
    private final BrandCampaignReactionRepository brandCampaignReactionRepository;
    private final BrandRepository brandRepository;
    private final CreatorRepository creatorRepository;
    private final NotificationService notificationService;
    private final ServicePackageRepository servicePackageRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final CampaignAlertRuleRepository campaignAlertRuleRepository;

    public BrandCampaignService(BrandCampaignRepository brandCampaignRepository,
                             BrandCampaignReactionRepository brandCampaignReactionRepository,
                             BrandRepository brandRepository,
                             CreatorRepository creatorRepository,
                             NotificationService notificationService,
                             ServicePackageRepository servicePackageRepository,
                             OrderRepository orderRepository,
                             OrderService orderService,
                             CampaignAlertRuleRepository campaignAlertRuleRepository) {
        this.brandCampaignRepository = brandCampaignRepository;
        this.brandCampaignReactionRepository = brandCampaignReactionRepository;
        this.brandRepository = brandRepository;
        this.creatorRepository = creatorRepository;
        this.notificationService = notificationService;
        this.servicePackageRepository = servicePackageRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.campaignAlertRuleRepository = campaignAlertRuleRepository;
    }

    // ── Brand: create / update ────────────────────────────────────────────────

    @Transactional
    public BrandCampaignResponse createCampaign(UUID brandId, UserRole role, BrandCampaignCreateRequest request) {
        requireBrand(role);
        String budgetType = normalizeBudgetType(request.budgetType());
        validateBudgetForType(budgetType, request.budgetMin(), request.budgetMax());
        validateTimelineWindow(request.deadlineDate(), request.contentSubmissionDeadline(), request.goLiveDate());
        String locationMode = normalizeLocationTargetingMode(request.locationTargetingMode());
        String targetCities = normalizeLocationList(request.targetCities());
        String targetRegion = trimToNull(request.targetRegion());
        validateLocationTargeting(locationMode, targetCities, targetRegion);

        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Brand profile not found"));

        if (brand.getPlanTier() == BrandPlanTier.STARTER) {
            Instant startOfMonth = YearMonth.now(ZoneOffset.UTC).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            long campaignsThisMonth = brandCampaignRepository.countByBrandIdAndCreatedAtAfter(brandId, startOfMonth);
            if (campaignsThisMonth >= 5) {
                throw new ApiException(HttpStatus.PAYMENT_REQUIRED,
                        "Starter plan allows 5 campaigns per month. Upgrade to Growth or Enterprise for unlimited campaigns.");
            }
        }

        BrandCampaign campaign = BrandCampaign.builder()
                 .brand(brand)
                 .title(request.title().trim())
                 .brief(request.brief().trim())
                 .offerType(request.offerType().trim())
                 .budgetMin(request.budgetMin() != null ? request.budgetMin() : 0)
                 .budgetMax(request.budgetMax() != null ? request.budgetMax() : 0)
                 .currency(normalizeCurrency(request.currency()))
                 .budgetType(budgetType)
                 .paymentStructure(normalizePaymentStructure(request.paymentStructure(), budgetType))
                 .barterProductDesc(request.barterProductDesc())
                 .barterEstimatedValue(request.barterEstimatedValue())
                 .travelCostsCovered(request.travelCostsCovered() != null && request.travelCostsCovered())
                 .deliverables(request.deliverables())
                 .contentFormats(request.contentFormats())
                 .targetPlatforms(request.targetPlatforms())
                 .campaignGoal(trimToNull(request.campaignGoal()))
                 .categories(request.categories())
                 .niches(request.niches())
                 .referenceUrls(request.referenceUrls())
                 .keyMessage(trimToNull(request.keyMessage()))
                 .dosAndDonts(trimToNull(request.dosAndDonts()))
                 .hashtagsMentions(trimToNull(request.hashtagsMentions()))
                 .usageRights(trimToNull(request.usageRights()))
                 .termsAndConditions(trimToNull(request.termsAndConditions()))
                 .expectedOutcomes(trimToNull(request.expectedOutcomes()))
                 .coverImageUrl(request.coverImageUrl())
                 .deadlineDate(request.deadlineDate())
                 .locationTargetingMode(locationMode)
                 .targetCities("cities".equals(locationMode) ? targetCities : null)
                 .targetRegion("region".equals(locationMode) ? targetRegion : null)
                 .targetCity(deriveLegacyTargetCity(locationMode, targetCities, targetRegion, request.targetCity()))
                 .targetLanguage(request.targetLanguage())
                 .visibility(normalizeVisibility(request.visibility()))
                 .creatorType(request.creatorType())
                 .followerRange(request.followerRange())
                 .creatorGenderPreference(request.creatorGenderPreference())
                 .minAge(request.minAge())
                 .maxAge(request.maxAge())
                 .applicationType(request.applicationType())
                 .maxApplicants(request.maxApplicants())
                 .minProposedPrice(request.minProposedPrice())
                 .proposalRequired(request.proposalRequired() != null && request.proposalRequired())
                 .portfolioRequired(request.portfolioRequired() != null && request.portfolioRequired())
                 .customScreeningQuestions(request.customScreeningQuestions())
                 .contentSubmissionDeadline(request.contentSubmissionDeadline())
                 .goLiveDate(request.goLiveDate())
                 .campaignDuration(request.campaignDuration())
                 .status(BrandCampaignStatus.DRAFT)
                 .build();

        return toCampaignResponse(brandCampaignRepository.save(campaign));
    }

    @Transactional
    public BrandCampaignResponse updateCampaign(UUID campaignId, UUID brandId, UserRole role, BrandCampaignUpdateRequest request) {
        requireBrand(role);
        BrandCampaign campaign = getOwnedCampaign(campaignId, brandId);
        if (campaign.getStatus() == BrandCampaignStatus.CLOSED || campaign.getStatus() == BrandCampaignStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Closed or archived campaigns cannot be edited");
        }

        String nextBudgetType = request.budgetType() != null
                ? normalizeBudgetType(request.budgetType())
                : campaign.getBudgetType();
        Integer nextMin = request.budgetMin() != null ? request.budgetMin() : campaign.getBudgetMin();
        Integer nextMax = request.budgetMax() != null ? request.budgetMax() : campaign.getBudgetMax();
        validateBudgetForType(nextBudgetType, nextMin, nextMax);
        LocalDate nextDeadline = request.deadlineDate() != null ? request.deadlineDate() : campaign.getDeadlineDate();
        LocalDate nextContentSubmissionDeadline = request.contentSubmissionDeadline() != null
                ? request.contentSubmissionDeadline()
                : campaign.getContentSubmissionDeadline();
        LocalDate nextGoLiveDate = request.goLiveDate() != null ? request.goLiveDate() : campaign.getGoLiveDate();
        validateTimelineWindow(nextDeadline, nextContentSubmissionDeadline, nextGoLiveDate);
        String nextLocationMode = request.locationTargetingMode() != null
                ? normalizeLocationTargetingMode(request.locationTargetingMode())
                : normalizeLocationTargetingMode(campaign.getLocationTargetingMode());
        String existingTargetCities = normalizeLocationList(campaign.getTargetCities());
        if (existingTargetCities == null
                && "cities".equals(normalizeLocationTargetingMode(campaign.getLocationTargetingMode()))
                && trimToNull(campaign.getTargetCity()) != null) {
            existingTargetCities = trimToNull(campaign.getTargetCity());
        }
        String nextTargetCities = request.targetCities() != null
                ? normalizeLocationList(request.targetCities())
                : existingTargetCities;
        String nextTargetRegion = request.targetRegion() != null
                ? trimToNull(request.targetRegion())
                : trimToNull(campaign.getTargetRegion());
        validateLocationTargeting(nextLocationMode, nextTargetCities, nextTargetRegion);

         if (request.title() != null) campaign.setTitle(request.title().trim());
         if (request.brief() != null) campaign.setBrief(request.brief().trim());
         if (request.offerType() != null) campaign.setOfferType(request.offerType().trim());
         if (request.budgetMin() != null) campaign.setBudgetMin(request.budgetMin());
         if (request.budgetMax() != null) campaign.setBudgetMax(request.budgetMax());
         if (request.currency() != null) campaign.setCurrency(normalizeCurrency(request.currency()));
         if (request.budgetType() != null) campaign.setBudgetType(normalizeBudgetType(request.budgetType()));
         if (request.paymentStructure() != null) campaign.setPaymentStructure(normalizePaymentStructure(request.paymentStructure(), campaign.getBudgetType()));
         if (request.barterProductDesc() != null) campaign.setBarterProductDesc(request.barterProductDesc());
         if (request.barterEstimatedValue() != null) campaign.setBarterEstimatedValue(request.barterEstimatedValue());
         if (request.travelCostsCovered() != null) campaign.setTravelCostsCovered(request.travelCostsCovered());
         if (request.deliverables() != null) campaign.setDeliverables(request.deliverables());
         if (request.contentFormats() != null) campaign.setContentFormats(request.contentFormats());
         if (request.targetPlatforms() != null) campaign.setTargetPlatforms(request.targetPlatforms());
         if (request.campaignGoal() != null) campaign.setCampaignGoal(trimToNull(request.campaignGoal()));
         if (request.categories() != null) campaign.setCategories(request.categories());
         if (request.niches() != null) campaign.setNiches(request.niches());
         if (request.referenceUrls() != null) campaign.setReferenceUrls(request.referenceUrls());
         if (request.keyMessage() != null) campaign.setKeyMessage(trimToNull(request.keyMessage()));
         if (request.dosAndDonts() != null) campaign.setDosAndDonts(trimToNull(request.dosAndDonts()));
         if (request.hashtagsMentions() != null) campaign.setHashtagsMentions(trimToNull(request.hashtagsMentions()));
         if (request.usageRights() != null) campaign.setUsageRights(trimToNull(request.usageRights()));
         if (request.termsAndConditions() != null) campaign.setTermsAndConditions(trimToNull(request.termsAndConditions()));
         if (request.expectedOutcomes() != null) campaign.setExpectedOutcomes(trimToNull(request.expectedOutcomes()));
         if (request.coverImageUrl() != null) campaign.setCoverImageUrl(request.coverImageUrl());
         if (request.deadlineDate() != null) campaign.setDeadlineDate(request.deadlineDate());
         if (request.locationTargetingMode() != null || request.targetCities() != null || request.targetRegion() != null || request.targetCity() != null) {
             campaign.setLocationTargetingMode(nextLocationMode);
             campaign.setTargetCities("cities".equals(nextLocationMode) ? nextTargetCities : null);
             campaign.setTargetRegion("region".equals(nextLocationMode) ? nextTargetRegion : null);
             campaign.setTargetCity(deriveLegacyTargetCity(nextLocationMode, nextTargetCities, nextTargetRegion, request.targetCity()));
         }
         if (request.targetLanguage() != null) campaign.setTargetLanguage(request.targetLanguage());
         if (request.visibility() != null) campaign.setVisibility(normalizeVisibility(request.visibility()));
         if (request.creatorType() != null) campaign.setCreatorType(request.creatorType());
         if (request.followerRange() != null) campaign.setFollowerRange(request.followerRange());
         if (request.creatorGenderPreference() != null) campaign.setCreatorGenderPreference(request.creatorGenderPreference());
         if (request.minAge() != null) campaign.setMinAge(request.minAge());
         if (request.maxAge() != null) campaign.setMaxAge(request.maxAge());
         if (request.applicationType() != null) campaign.setApplicationType(request.applicationType());
         if (request.maxApplicants() != null) campaign.setMaxApplicants(request.maxApplicants());
         if (request.minProposedPrice() != null) campaign.setMinProposedPrice(request.minProposedPrice());
         if (request.proposalRequired() != null) campaign.setProposalRequired(request.proposalRequired());
         if (request.portfolioRequired() != null) campaign.setPortfolioRequired(request.portfolioRequired());
         if (request.customScreeningQuestions() != null) campaign.setCustomScreeningQuestions(request.customScreeningQuestions());
         if (request.contentSubmissionDeadline() != null) campaign.setContentSubmissionDeadline(request.contentSubmissionDeadline());
         if (request.goLiveDate() != null) campaign.setGoLiveDate(request.goLiveDate());
         if (request.campaignDuration() != null) campaign.setCampaignDuration(request.campaignDuration());

        return toCampaignResponse(brandCampaignRepository.save(campaign));
    }

    @Transactional
    public BrandCampaignResponse updateCampaignStatus(UUID campaignId, UUID brandId, UserRole role,
                                                BrandCampaignStatusUpdateRequest request) {
        requireBrand(role);
        BrandCampaign campaign = getOwnedCampaign(campaignId, brandId);
        BrandCampaignStatus next = parseStatus(request.status());
        BrandCampaignStatus current = campaign.getStatus();
        if (current == next) return toCampaignResponse(campaign);

        switch (next) {
            case DRAFT -> throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Cannot move back to DRAFT");
            case PUBLISHED -> {
                if (current != BrandCampaignStatus.DRAFT && current != BrandCampaignStatus.PAUSED) {
                    throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Only draft or paused campaigns can be published");
                }
                campaign.setPublishedAt(Instant.now());
                campaign.setClosedAt(null);
            }
            case PAUSED -> {
                if (current != BrandCampaignStatus.PUBLISHED) {
                    throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Only published campaigns can be paused");
                }
            }
            case CLOSED -> {
                if (current != BrandCampaignStatus.PUBLISHED && current != BrandCampaignStatus.PAUSED) {
                    throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Only published or paused campaigns can be closed");
                }
                campaign.setClosedAt(Instant.now());
            }
            case ARCHIVED -> {
                if (current != BrandCampaignStatus.CLOSED) {
                    throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Only closed campaigns can be archived");
                }
            }
        }

        campaign.setStatus(next);
        return toCampaignResponse(brandCampaignRepository.save(campaign));
    }

    // ── Brand: listing ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<BrandCampaignResponse> listBrandCampaigns(UUID brandId, UserRole role, int page, int size, String status) {
        requireBrand(role);
        if (status != null && !status.isBlank()) {
            BrandCampaignStatus campaignStatus = BrandCampaignStatus.valueOf(status.trim().toUpperCase());
            return PageResponse.from(
                    brandCampaignRepository.findByBrandIdAndStatusOrderByCreatedAtDesc(brandId, campaignStatus, safePage(page, size))
                            .map(this::toCampaignResponse)
            );
        }
        return PageResponse.from(
                brandCampaignRepository.findByBrandIdOrderByCreatedAtDesc(brandId, safePage(page, size))
                        .map(this::toCampaignResponse)
        );
    }

    @Transactional(readOnly = true)
    public BrandCampaignResponse getBrandCampaign(UUID campaignId, UUID brandId, UserRole role) {
        requireBrand(role);
        return toCampaignResponse(getOwnedCampaign(campaignId, brandId));
    }

    // ── Brand: reaction inbox ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<BrandCampaignReactionResponse> listCampaignReactions(UUID campaignId, UUID brandId, UserRole role,
                                                                        String status, String reactionType,
                                                                        int page, int size) {
        requireBrand(role);
        getOwnedCampaign(campaignId, brandId);
        return PageResponse.from(
                brandCampaignReactionRepository.findByCampaignIdWithFilters(
                        campaignId,
                        status != null ? BrandCampaignReactionStatus.valueOf(status.trim().toUpperCase()) : null,
                        reactionType != null ? BrandCampaignReactionType.valueOf(reactionType.trim().toUpperCase()) : null,
                        safePage(page, size))
                        .map(this::toReactionResponse)
        );
    }

    @Transactional
    public BrandCampaignReactionResponse actionReaction(UUID campaignId, UUID reactionId, UUID brandId,
                                                     UserRole role, BrandCampaignReactionActionRequest request) {
        requireBrand(role);
        BrandCampaign campaign = getOwnedCampaign(campaignId, brandId);

        BrandCampaignReaction reaction = brandCampaignReactionRepository.findById(reactionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reaction not found"));
        if (!reaction.getCampaign().getId().equals(campaignId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Reaction does not belong to this campaign");
        }
        if (reaction.getStatus() == BrandCampaignReactionStatus.ACCEPTED
                || reaction.getStatus() == BrandCampaignReactionStatus.REJECTED
                || reaction.getStatus() == BrandCampaignReactionStatus.WITHDRAWN) {
            throw new ApiException(HttpStatus.CONFLICT, "Finalized reactions cannot be updated");
        }

        BrandCampaignReactionStatus targetStatus = switch (request.action().trim().toUpperCase(Locale.ROOT)) {
            case "SHORTLIST"           -> BrandCampaignReactionStatus.SHORTLISTED;
            case "REVIEW", "IN_REVIEW" -> BrandCampaignReactionStatus.IN_REVIEW;
            case "ACCEPT", "ACCEPTED"  -> BrandCampaignReactionStatus.ACCEPTED;
            case "REJECT", "REJECTED"  -> BrandCampaignReactionStatus.REJECTED;
            default -> throw new ApiException(HttpStatus.BAD_REQUEST,
                    "action must be SHORTLIST, REVIEW, ACCEPT, or REJECT");
        };
        if (targetStatus == BrandCampaignReactionStatus.ACCEPTED
                && reaction.getReactionType() != BrandCampaignReactionType.PROPOSAL
                && reaction.getReactionType() != BrandCampaignReactionType.INTERESTED) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Only interested or proposal responses can be accepted into an order");
        }

        reaction.setStatus(targetStatus);
        if (request.brandNote() != null) reaction.setBrandNote(request.brandNote());

        BrandCampaignReaction saved = brandCampaignReactionRepository.save(reaction);
        UUID orderId = null;
        if (targetStatus == BrandCampaignReactionStatus.ACCEPTED) {
            ServicePackage campaignPackage = createAcceptedCampaignPackage(saved);
            orderId = orderService.createPrivateDealOrder(
                    campaignPackage.getId(),
                    campaign.getBrand().getId(),
                    effectiveAmount(saved, campaign),
                    effectiveBarterDetails(campaign),
                    acceptedCampaignMessage(saved, campaign),
                    effectiveDealType(campaign)
            ).id();
        }
        notifyCreatorOfReactionAction(saved, campaign, targetStatus, request.brandNote());
        evaluateAlertRules(campaign);
        return toReactionResponse(saved, orderId);
    }

    // ── Creator: discovery feed ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<BrandCampaignResponse> listCreatorFeed(UserRole role,
                                                             String search, String city, String offerType, String platform, String campaignGoal,
                                                             Integer budgetMin, Integer budgetMax,
                                                             int page, int size) {
        requireCreator(role);
        return PageResponse.from(
                brandCampaignRepository.findPublishedForCreatorFeed(
                        trimToNull(search), trimToNull(city), trimToNull(offerType),
                        trimToNull(platform), trimToNull(campaignGoal),
                        budgetMin, budgetMax, LocalDate.now(), safePage(page, size))
                        .map(this::toCampaignResponse)
        );
    }

    @Transactional(readOnly = true)
    public BrandCampaignResponse getCreatorCampaign(UUID campaignId, UserRole role) {
        requireCreator(role);
        BrandCampaign campaign = brandCampaignRepository.findById(campaignId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Campaign not found"));
        if (campaign.getStatus() != BrandCampaignStatus.PUBLISHED) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Campaign not found");
        }
        if (campaign.getDeadlineDate() != null && campaign.getDeadlineDate().isBefore(LocalDate.now())) {
            throw new ApiException(HttpStatus.GONE, "Campaign is expired");
        }
        return toCampaignResponse(campaign);
    }

    // ── Creator: react ────────────────────────────────────────────────────────

    @Transactional
    public BrandCampaignReactionResponse reactToCampaign(UUID campaignId, UUID creatorId, UserRole role,
                                                   BrandCampaignReactionCreateRequest request) {
        requireCreator(role);
        BrandCampaign campaign = brandCampaignRepository.findById(campaignId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Campaign not found"));
        if (campaign.getStatus() != BrandCampaignStatus.PUBLISHED) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "This campaign is not accepting reactions");
        }
        if (campaign.getDeadlineDate() != null && campaign.getDeadlineDate().isBefore(LocalDate.now())) {
            throw new ApiException(HttpStatus.GONE, "Campaign deadline has passed");
        }

        Creator creator = creatorRepository.findById(creatorId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator profile not found"));

        BrandCampaignReactionType reactionType = parseReactionType(request.reactionType());
        validateReactionPayload(reactionType, request.proposedPrice(), request.proposedDeliveryDays(), request.message());

        if (campaign.getMinProposedPrice() != null
                && request.proposedPrice() != null
                && request.proposedPrice() < campaign.getMinProposedPrice()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "Your proposed price is below the campaign minimum of PKR " + campaign.getMinProposedPrice());
        }

        BrandCampaignReaction reaction = brandCampaignReactionRepository
                .findByCampaignIdAndCreatorId(campaignId, creatorId)
                .orElse(BrandCampaignReaction.builder().campaign(campaign).creator(creator).build());

        reaction.setReactionType(reactionType);
        reaction.setStatus(BrandCampaignReactionStatus.SUBMITTED);
        reaction.setMessage(request.message());
        reaction.setProposedPrice(request.proposedPrice());
        reaction.setProposedCurrency(normalizeCurrency(request.proposedCurrency()));
        reaction.setProposedDeliveryDays(request.proposedDeliveryDays());
        reaction.setCreatorNote(request.creatorNote());

        BrandCampaignReaction saved = brandCampaignReactionRepository.save(reaction);

        String notifTitle = creator.getName() + " reacted to your campaign";
        String notifBody = "\"" + campaign.getTitle() + "\" — " + reactionType.name().toLowerCase(Locale.ROOT)
                + (request.message() != null && !request.message().isBlank()
                ? ": " + truncate(request.message(), 120) : "");
        notificationService.send(campaign.getBrand().getId(), "CAMPAIGN_REACTION", notifTitle, notifBody,
                "BRAND_CAMPAIGN", campaign.getId());
        evaluateAlertRules(campaign);

        return toReactionResponse(saved);
    }

    @Transactional
    public BrandCampaignReactionResponse updateCreatorReaction(UUID campaignId, UUID reactionId, UUID creatorId,
                                                            UserRole role, BrandCampaignReactionUpdateRequest request) {
        requireCreator(role);
        BrandCampaignReaction reaction = brandCampaignReactionRepository.findById(reactionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reaction not found"));
        if (!reaction.getCampaign().getId().equals(campaignId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Reaction does not belong to this campaign");
        }
        if (!reaction.getCreator().getId().equals(creatorId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can update only your own reaction");
        }
        if (reaction.getStatus() == BrandCampaignReactionStatus.ACCEPTED
                || reaction.getStatus() == BrandCampaignReactionStatus.REJECTED) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Finalized reactions cannot be edited");
        }

        if (request.message() != null) reaction.setMessage(request.message());
        if (request.proposedPrice() != null) reaction.setProposedPrice(request.proposedPrice());
        if (request.proposedCurrency() != null) reaction.setProposedCurrency(normalizeCurrency(request.proposedCurrency()));
        if (request.proposedDeliveryDays() != null) reaction.setProposedDeliveryDays(request.proposedDeliveryDays());
        if (request.creatorNote() != null) reaction.setCreatorNote(request.creatorNote());

        if (request.status() != null && !request.status().isBlank()) {
            BrandCampaignReactionStatus next = parseReactionStatus(request.status());
            if (next != BrandCampaignReactionStatus.WITHDRAWN && next != BrandCampaignReactionStatus.SUBMITTED) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Creator can only set status to submitted or withdrawn");
            }
            reaction.setStatus(next);
        }

        return toReactionResponse(brandCampaignReactionRepository.save(reaction));
    }

    @Transactional(readOnly = true)
    public PageResponse<BrandCampaignReactionResponse> listCreatorReactions(UUID creatorId, UserRole role,
                                                                          int page, int size) {
        requireCreator(role);
        return PageResponse.from(
                brandCampaignReactionRepository.findByCreatorIdWithCampaign(creatorId, safePage(page, size))
                        .map(this::toReactionResponse)
        );
    }

    @Transactional
    public void evaluateAlertRules(UUID campaignId) {
        BrandCampaign campaign = brandCampaignRepository.findById(campaignId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Campaign not found"));
        evaluateAlertRules(campaign);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private BrandCampaign getOwnedCampaign(UUID campaignId, UUID brandId) {
        BrandCampaign campaign = brandCampaignRepository.findById(campaignId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Campaign not found"));
        if (!campaign.getBrand().getId().equals(brandId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not own this campaign");
        }
        return campaign;
    }

    private void evaluateAlertRules(BrandCampaign campaign) {
        List<CampaignAlertRule> rules = campaignAlertRuleRepository.findByCampaignIdAndActiveTrue(campaign.getId());
        if (rules.isEmpty()) return;

        long reactionCount = brandCampaignReactionRepository.countByCampaignId(campaign.getId());
        long acceptedCount = brandCampaignReactionRepository.countByCampaignIdAndStatus(campaign.getId(), BrandCampaignReactionStatus.ACCEPTED);
        double acceptanceRate = reactionCount > 0 ? acceptedCount * 100.0 / reactionCount : 0.0;
        long projectedSpend = brandCampaignReactionRepository.findByCampaignIdWithCreator(campaign.getId()).stream()
                .filter(reaction -> reaction.getProposedPrice() != null)
                .mapToLong(BrandCampaignReaction::getProposedPrice)
                .sum();
        long budgetMax = campaign.getBudgetMax() == null ? 0 : campaign.getBudgetMax();

        for (CampaignAlertRule rule : rules) {
            String type = rule.getType() == null ? "" : rule.getType().trim().toLowerCase(Locale.ROOT);
            boolean triggered = switch (type) {
                case "reaction_threshold" -> reactionCount >= rule.getThreshold();
                case "no_reactions" -> reactionCount == 0 && noReactionWindowElapsed(campaign, rule.getThreshold());
                case "low_acceptance_rate" -> reactionCount > 0 && acceptanceRate <= rule.getThreshold();
                case "spend_exceeded" -> budgetMax > 0 && projectedSpend >= budgetMax + Math.round(budgetMax * (rule.getThreshold() / 100.0));
                default -> false;
            };
            if (triggered && shouldTrigger(rule)) {
                rule.setLastTriggeredAt(Instant.now());
                campaignAlertRuleRepository.save(rule);
                notificationService.send(
                        campaign.getBrand().getId(),
                        "CAMPAIGN_ALERT",
                        "Campaign alert triggered",
                        alertBody(campaign, rule, reactionCount, acceptanceRate, projectedSpend),
                        "BRAND_CAMPAIGN",
                        campaign.getId()
                );
            }
        }
    }

    private boolean shouldTrigger(CampaignAlertRule rule) {
        return rule.getLastTriggeredAt() == null || rule.getLastTriggeredAt().isBefore(Instant.now().minusSeconds(3600));
    }

    private boolean noReactionWindowElapsed(BrandCampaign campaign, int days) {
        Instant start = campaign.getPublishedAt() != null ? campaign.getPublishedAt() : campaign.getCreatedAt();
        if (start == null) return false;
        return !Instant.now().isBefore(start.plus(Math.max(1, days), ChronoUnit.DAYS));
    }

    private String alertBody(BrandCampaign campaign, CampaignAlertRule rule, long reactions, double acceptanceRate, long spend) {
        String type = rule.getType() == null ? "alert" : rule.getType().replace('_', ' ');
        return "\"" + campaign.getTitle() + "\" reached " + type
                + " (threshold " + rule.getThreshold() + "; reactions " + reactions
                + "; acceptance " + Math.round(acceptanceRate) + "%; projected spend PKR " + spend + ").";
    }

    private void notifyCreatorOfReactionAction(BrandCampaignReaction reaction, BrandCampaign campaign,
                                               BrandCampaignReactionStatus status, String brandNote) {
        String title = switch (status) {
            case SHORTLISTED -> campaign.getBrand().getDisplayName() + " shortlisted your pitch";
            case IN_REVIEW   -> campaign.getBrand().getDisplayName() + " is reviewing your proposal";
            case ACCEPTED    -> "🎉 " + campaign.getBrand().getDisplayName() + " accepted your proposal!";
            case REJECTED    -> campaign.getBrand().getDisplayName() + " passed on your pitch";
            default          -> campaign.getBrand().getDisplayName() + " updated your reaction";
        };
        String body = "Campaign: \"" + campaign.getTitle() + "\""
                + (brandNote != null && !brandNote.isBlank() ? " — Note: " + truncate(brandNote, 120) : "");
        notificationService.send(reaction.getCreator().getId(), "CAMPAIGN_REACTION_UPDATE",
                title, body, "BRAND_CAMPAIGN_REACTION", reaction.getId());
    }

    private Pageable safePage(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }

    private BrandCampaignStatus parseStatus(String value) {
        try { return BrandCampaignStatus.valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (Exception ex) { throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid campaign status: " + value); }
    }

    private BrandCampaignReactionType parseReactionType(String value) {
        try { return BrandCampaignReactionType.valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (Exception ex) { throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid reaction type: " + value); }
    }

    private BrandCampaignReactionStatus parseReactionStatus(String value) {
        try { return BrandCampaignReactionStatus.valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (Exception ex) { throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid reaction status: " + value); }
    }

    private void requireBrand(UserRole role) {
        if (!role.isBrand()) throw new ApiException(HttpStatus.FORBIDDEN, "Only brands can perform this action");
    }

    private void requireCreator(UserRole role) {
        if (!role.isCreator()) throw new ApiException(HttpStatus.FORBIDDEN, "Only creators can perform this action");
    }

    private void validateBudgetForType(String budgetType, Integer min, Integer max) {
        // Barter-only campaigns don't need a monetary budget
        if ("barter_only".equals(budgetType)) return;
        if (min == null || max == null || min < 0 || max < 0 || min > max) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "budgetMin and budgetMax must be valid and budgetMin <= budgetMax");
        }
    }

    private void validateTimelineWindow(LocalDate applicationDeadline,
                                        LocalDate contentSubmissionDeadline,
                                        LocalDate goLiveDate) {
        if (applicationDeadline != null && contentSubmissionDeadline != null
                && applicationDeadline.isAfter(contentSubmissionDeadline)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Application deadline must be on or before content submission deadline");
        }
        if (contentSubmissionDeadline != null && goLiveDate != null
                && contentSubmissionDeadline.isAfter(goLiveDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Content submission deadline must be on or before go-live date");
        }
        if (applicationDeadline != null && goLiveDate != null
                && applicationDeadline.isAfter(goLiveDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Application deadline must be on or before go-live date");
        }
    }

    private String normalizeLocationTargetingMode(String value) {
        String next = trimToNull(value);
        if (next == null) return "nationwide";
        return switch (next.toLowerCase(Locale.ROOT)) {
            case "region" -> "region";
            case "cities" -> "cities";
            case "remote_only" -> "remote_only";
            default -> "nationwide";
        };
    }

    private String normalizeLocationList(String value) {
        String next = trimToNull(value);
        if (next == null) return null;
        return Arrays.stream(next.split("[\\n,]"))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .distinct()
                .collect(Collectors.joining(", "));
    }

    private void validateLocationTargeting(String mode, String targetCities, String targetRegion) {
        if ("cities".equals(mode) && trimToNull(targetCities) == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select at least one target city when targeting by cities");
        }
        if ("region".equals(mode) && trimToNull(targetRegion) == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select a target region when targeting by region");
        }
    }

    private String deriveLegacyTargetCity(String mode, String targetCities, String targetRegion, String fallback) {
        return switch (mode) {
            case "remote_only" -> "Remote / Online Only";
            case "region" -> trimToNull(targetRegion);
            case "cities" -> {
                String normalized = normalizeLocationList(targetCities);
                if (normalized == null) yield trimToNull(fallback);
                String[] entries = normalized.split(",\\s*");
                if (entries.length == 1) yield entries[0];
                yield entries.length + " cities";
            }
            default -> "Nationwide";
        };
    }

    private String normalizeBudgetType(String value) {
        if (value == null) return "fixed";
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "open_to_bids" -> "open_to_bids";
            case "paid_and_barter" -> "paid_and_barter";
            case "barter_only" -> "barter_only";
            default -> "fixed";
        };
    }

    private String normalizePaymentStructure(String value, String budgetType) {
        // Barter-only campaigns don't have a cash payment structure
        if ("barter_only".equals(budgetType)) return null;
        if (value == null) return "full_upfront";
        return "split_50_50".equals(value.trim().toLowerCase(Locale.ROOT)) ? "split_50_50" : "full_upfront";
    }

    private void validateReactionPayload(BrandCampaignReactionType type, Integer proposedPrice,
                                         Integer proposedDeliveryDays, String message) {
        if (type == BrandCampaignReactionType.PROPOSAL) {
            if (proposedPrice == null || proposedPrice <= 0)
                throw new ApiException(HttpStatus.BAD_REQUEST, "proposedPrice is required for PROPOSAL");
            if (proposedDeliveryDays == null || proposedDeliveryDays <= 0)
                throw new ApiException(HttpStatus.BAD_REQUEST, "proposedDeliveryDays is required for PROPOSAL");
        }
        if ((type == BrandCampaignReactionType.QUESTION || type == BrandCampaignReactionType.DECLINE)
                && (message == null || message.isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "message is required for QUESTION and DECLINE");
        }
    }

    private String normalizeCurrency(String value) {
        String v = trimToNull(value);
        return v == null ? "PKR" : v.toUpperCase(Locale.ROOT);
    }

    private String normalizeVisibility(String value) {
        String v = trimToNull(value);
        if (v == null) return "public";
        String next = v.toLowerCase(Locale.ROOT);
        return "private".equals(next) ? "private" : "public";
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "…";
    }

    private ServicePackage createAcceptedCampaignPackage(BrandCampaignReaction reaction) {
        BrandCampaign campaign = reaction.getCampaign();
        Creator creator = reaction.getCreator();
        String packageName = "brand-campaign-" + reaction.getId();

        return servicePackageRepository.save(ServicePackage.builder()
                .creator(creator)
                .name(packageName)
                .title(truncate(campaign.getTitle(), 200))
                .shortDescription("Accepted brand campaign")
                .description(campaign.getBrief())
                .fullDescription(campaign.getBrief())
                .platform(resolvePlatform(campaign.getTargetPlatforms()))
                .category(PackageCategory.GENERAL)
                .dealType(effectiveDealType(campaign))
                .status(PackageStatus.ACTIVE)
                .visibility("private")
                .price(effectiveAmount(reaction, campaign))
                .barterDetails(effectiveBarterDetails(campaign))
                .barterDescription(effectiveBarterDetails(campaign))
                .estimatedBarterValue(campaign.getBarterEstimatedValue())
                .hybridCashAmount(effectiveDealType(campaign) == DealType.HYBRID ? effectiveAmount(reaction, campaign) : null)
                .hybridBarterValue(campaign.getBarterEstimatedValue())
                .creatorExpectations(reaction.getCreatorNote())
                .deliverables(parseDeliverables(campaign.getDeliverables()))
                .deliveryDays(effectiveDeliveryDays(reaction, campaign))
                .revisions(1)
                .tags(List.of("brand-campaign"))
                .currency(normalizeCurrency(reaction.getProposedCurrency() != null
                        ? reaction.getProposedCurrency() : campaign.getCurrency()))
                .responseTime("Within 24 hours")
                .active(true)
                .build());
    }

    private DealType effectiveDealType(BrandCampaign campaign) {
        return switch (normalizeBudgetType(campaign.getBudgetType())) {
            case "barter_only" -> DealType.BARTER;
            case "paid_and_barter" -> DealType.HYBRID;
            default -> DealType.PAID;
        };
    }

    private int effectiveAmount(BrandCampaignReaction reaction, BrandCampaign campaign) {
        if (effectiveDealType(campaign) == DealType.BARTER) return 0;
        if (reaction.getProposedPrice() != null && reaction.getProposedPrice() > 0) return reaction.getProposedPrice();
        if (campaign.getBudgetMax() != null && campaign.getBudgetMax() > 0) return campaign.getBudgetMax();
        throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Accepted paid campaigns require a valid amount");
    }

    private String effectiveBarterDetails(BrandCampaign campaign) {
        return effectiveDealType(campaign) == DealType.PAID ? null : trimToNull(campaign.getBarterProductDesc());
    }

    private int effectiveDeliveryDays(BrandCampaignReaction reaction, BrandCampaign campaign) {
        if (reaction.getProposedDeliveryDays() != null && reaction.getProposedDeliveryDays() > 0) {
            return reaction.getProposedDeliveryDays();
        }
        if (campaign.getCampaignDuration() != null && campaign.getCampaignDuration() > 0) {
            return campaign.getCampaignDuration();
        }
        return 7;
    }

    private List<String> parseDeliverables(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) return List.of("Campaign deliverable");
        List<String> result = Arrays.stream(normalized.split("[\\n,]"))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .distinct()
                .toList();
        return result.isEmpty() ? List.of("Campaign deliverable") : result;
    }

    private PackagePlatform resolvePlatform(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) return PackagePlatform.INSTAGRAM;
        String first = normalized.split("[\\n,]")[0].trim().toUpperCase(Locale.ROOT);
        try {
            return PackagePlatform.valueOf(first);
        } catch (IllegalArgumentException ex) {
            return PackagePlatform.INSTAGRAM;
        }
    }

    private String acceptedCampaignMessage(BrandCampaignReaction reaction, BrandCampaign campaign) {
        String proposal = trimToNull(reaction.getMessage());
        return proposal == null ? "Accepted response for " + campaign.getTitle() : proposal;
    }

     private BrandCampaignResponse toCampaignResponse(BrandCampaign campaign) {
         return new BrandCampaignResponse(
                 campaign.getId(),
                 campaign.getBrand().getId(),
                 campaign.getBrand().getDisplayName(),
                 campaign.getTitle(),
                 campaign.getBrief(),
                 campaign.getOfferType(),
                 campaign.getBudgetMin(),
                 campaign.getBudgetMax(),
                 campaign.getCurrency(),
                 campaign.getBudgetType(),
                 campaign.getPaymentStructure(),
                 campaign.getBarterProductDesc(),
                 campaign.getBarterEstimatedValue(),
                 campaign.getTravelCostsCovered(),
                 campaign.getDeliverables(),
                 campaign.getContentFormats(),
                 campaign.getTargetPlatforms(),
                 campaign.getCampaignGoal(),
                 campaign.getCategories(),
                 campaign.getNiches(),
                 campaign.getReferenceUrls(),
                 campaign.getKeyMessage(),
                 campaign.getDosAndDonts(),
                 campaign.getHashtagsMentions(),
                 campaign.getUsageRights(),
                 campaign.getTermsAndConditions(),
                 campaign.getExpectedOutcomes(),
                 campaign.getCoverImageUrl(),
                 campaign.getDeadlineDate(),
                 campaign.getLocationTargetingMode(),
                 campaign.getTargetCities(),
                 campaign.getTargetRegion(),
                 campaign.getTargetCity(),
                 campaign.getTargetLanguage(),
                 campaign.getVisibility(),
                 campaign.getCreatorType(),
                 campaign.getFollowerRange(),
                 campaign.getCreatorGenderPreference(),
                 campaign.getMinAge(),
                 campaign.getMaxAge(),
                 campaign.getApplicationType(),
                 campaign.getMaxApplicants(),
                 campaign.getMinProposedPrice(),
                 campaign.getProposalRequired(),
                 campaign.getPortfolioRequired(),
                 campaign.getCustomScreeningQuestions(),
                 campaign.getContentSubmissionDeadline(),
                 campaign.getGoLiveDate(),
                 campaign.getCampaignDuration(),
                 campaign.getStatus().name().toLowerCase(Locale.ROOT),
                 campaign.getPublishedAt(),
                 campaign.getClosedAt(),
                 campaign.getCreatedAt(),
                 campaign.getUpdatedAt(),
                 brandCampaignReactionRepository.countByCampaignId(campaign.getId())
         );
     }

    private BrandCampaignReactionResponse toReactionResponse(BrandCampaignReaction reaction) {
        UUID orderId = orderRepository.findFirstByServicePackageName("brand-campaign-" + reaction.getId())
                .map(order -> order.getId())
                .orElse(null);
        return toReactionResponse(reaction, orderId);
    }

    private BrandCampaignReactionResponse toReactionResponse(BrandCampaignReaction reaction, UUID orderId) {
        return new BrandCampaignReactionResponse(
                reaction.getId(),
                reaction.getCampaign().getId(),
                reaction.getCampaign().getTitle(),
                reaction.getCampaign().getBrand().getDisplayName(),
                reaction.getCreator().getId(),
                reaction.getCreator().getName(),
                reaction.getCreator().getAvatarUrl(),
                reaction.getReactionType().name().toLowerCase(Locale.ROOT),
                reaction.getStatus().name().toLowerCase(Locale.ROOT),
                reaction.getMessage(),
                reaction.getProposedPrice(),
                reaction.getProposedCurrency(),
                reaction.getProposedDeliveryDays(),
                reaction.getBrandNote(),
                reaction.getCreatorNote(),
                orderId,
                reaction.getCreatedAt(),
                reaction.getUpdatedAt()
        );
    }
}
