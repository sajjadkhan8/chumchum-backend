package com.chamcham.backend.service;

import com.chamcham.backend.dto.offer.BrandOfferCreateRequest;
import com.chamcham.backend.dto.offer.BrandOfferReactionActionRequest;
import com.chamcham.backend.dto.offer.BrandOfferReactionCreateRequest;
import com.chamcham.backend.dto.offer.BrandOfferReactionResponse;
import com.chamcham.backend.dto.offer.BrandOfferReactionUpdateRequest;
import com.chamcham.backend.dto.offer.BrandOfferResponse;
import com.chamcham.backend.dto.offer.BrandOfferStatusUpdateRequest;
import com.chamcham.backend.dto.offer.BrandOfferUpdateRequest;
import com.chamcham.backend.entity.Brand;
import com.chamcham.backend.entity.BrandOffer;
import com.chamcham.backend.entity.BrandOfferReaction;
import com.chamcham.backend.entity.Creator;
import com.chamcham.backend.entity.ServicePackage;
import com.chamcham.backend.entity.enums.BrandOfferReactionStatus;
import com.chamcham.backend.entity.enums.BrandOfferReactionType;
import com.chamcham.backend.entity.enums.BrandOfferStatus;
import com.chamcham.backend.entity.enums.DealType;
import com.chamcham.backend.entity.enums.PackagePlatform;
import com.chamcham.backend.entity.enums.PackageStatus;
import com.chamcham.backend.entity.enums.PackageType;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.repository.BrandOfferReactionRepository;
import com.chamcham.backend.repository.BrandOfferRepository;
import com.chamcham.backend.repository.BrandRepository;
import com.chamcham.backend.repository.CreatorRepository;
import com.chamcham.backend.repository.OrderRepository;
import com.chamcham.backend.repository.ServicePackageRepository;
import com.chamcham.backend.util.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class BrandOfferService {

    private static final int MAX_PAGE_SIZE = 50;

    private final BrandOfferRepository brandOfferRepository;
    private final BrandOfferReactionRepository brandOfferReactionRepository;
    private final BrandRepository brandRepository;
    private final CreatorRepository creatorRepository;
    private final NotificationService notificationService;
    private final ServicePackageRepository servicePackageRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public BrandOfferService(BrandOfferRepository brandOfferRepository,
                             BrandOfferReactionRepository brandOfferReactionRepository,
                             BrandRepository brandRepository,
                             CreatorRepository creatorRepository,
                             NotificationService notificationService,
                             ServicePackageRepository servicePackageRepository,
                             OrderRepository orderRepository,
                             OrderService orderService) {
        this.brandOfferRepository = brandOfferRepository;
        this.brandOfferReactionRepository = brandOfferReactionRepository;
        this.brandRepository = brandRepository;
        this.creatorRepository = creatorRepository;
        this.notificationService = notificationService;
        this.servicePackageRepository = servicePackageRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    // ── Brand: create / update ────────────────────────────────────────────────

    @Transactional
    public BrandOfferResponse createOffer(UUID brandId, UserRole role, BrandOfferCreateRequest request) {
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

        BrandOffer offer = BrandOffer.builder()
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
                 .proposalRequired(request.proposalRequired() != null && request.proposalRequired())
                 .portfolioRequired(request.portfolioRequired() != null && request.portfolioRequired())
                 .customScreeningQuestions(request.customScreeningQuestions())
                 .contentSubmissionDeadline(request.contentSubmissionDeadline())
                 .goLiveDate(request.goLiveDate())
                 .campaignDuration(request.campaignDuration())
                 .status(BrandOfferStatus.DRAFT)
                 .build();

        return toOfferResponse(brandOfferRepository.save(offer));
    }

    @Transactional
    public BrandOfferResponse updateOffer(UUID offerId, UUID brandId, UserRole role, BrandOfferUpdateRequest request) {
        requireBrand(role);
        BrandOffer offer = getOwnedOffer(offerId, brandId);
        if (offer.getStatus() == BrandOfferStatus.CLOSED || offer.getStatus() == BrandOfferStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Closed or archived offers cannot be edited");
        }

        String nextBudgetType = request.budgetType() != null
                ? normalizeBudgetType(request.budgetType())
                : offer.getBudgetType();
        Integer nextMin = request.budgetMin() != null ? request.budgetMin() : offer.getBudgetMin();
        Integer nextMax = request.budgetMax() != null ? request.budgetMax() : offer.getBudgetMax();
        validateBudgetForType(nextBudgetType, nextMin, nextMax);
        LocalDate nextDeadline = request.deadlineDate() != null ? request.deadlineDate() : offer.getDeadlineDate();
        LocalDate nextContentSubmissionDeadline = request.contentSubmissionDeadline() != null
                ? request.contentSubmissionDeadline()
                : offer.getContentSubmissionDeadline();
        LocalDate nextGoLiveDate = request.goLiveDate() != null ? request.goLiveDate() : offer.getGoLiveDate();
        validateTimelineWindow(nextDeadline, nextContentSubmissionDeadline, nextGoLiveDate);
        String nextLocationMode = request.locationTargetingMode() != null
                ? normalizeLocationTargetingMode(request.locationTargetingMode())
                : normalizeLocationTargetingMode(offer.getLocationTargetingMode());
        String existingTargetCities = normalizeLocationList(offer.getTargetCities());
        if (existingTargetCities == null
                && "cities".equals(normalizeLocationTargetingMode(offer.getLocationTargetingMode()))
                && trimToNull(offer.getTargetCity()) != null) {
            existingTargetCities = trimToNull(offer.getTargetCity());
        }
        String nextTargetCities = request.targetCities() != null
                ? normalizeLocationList(request.targetCities())
                : existingTargetCities;
        String nextTargetRegion = request.targetRegion() != null
                ? trimToNull(request.targetRegion())
                : trimToNull(offer.getTargetRegion());
        validateLocationTargeting(nextLocationMode, nextTargetCities, nextTargetRegion);

         if (request.title() != null) offer.setTitle(request.title().trim());
         if (request.brief() != null) offer.setBrief(request.brief().trim());
         if (request.offerType() != null) offer.setOfferType(request.offerType().trim());
         if (request.budgetMin() != null) offer.setBudgetMin(request.budgetMin());
         if (request.budgetMax() != null) offer.setBudgetMax(request.budgetMax());
         if (request.currency() != null) offer.setCurrency(normalizeCurrency(request.currency()));
         if (request.budgetType() != null) offer.setBudgetType(normalizeBudgetType(request.budgetType()));
         if (request.paymentStructure() != null) offer.setPaymentStructure(normalizePaymentStructure(request.paymentStructure(), offer.getBudgetType()));
         if (request.barterProductDesc() != null) offer.setBarterProductDesc(request.barterProductDesc());
         if (request.barterEstimatedValue() != null) offer.setBarterEstimatedValue(request.barterEstimatedValue());
         if (request.travelCostsCovered() != null) offer.setTravelCostsCovered(request.travelCostsCovered());
         if (request.deliverables() != null) offer.setDeliverables(request.deliverables());
         if (request.contentFormats() != null) offer.setContentFormats(request.contentFormats());
         if (request.targetPlatforms() != null) offer.setTargetPlatforms(request.targetPlatforms());
         if (request.campaignGoal() != null) offer.setCampaignGoal(trimToNull(request.campaignGoal()));
         if (request.categories() != null) offer.setCategories(request.categories());
         if (request.niches() != null) offer.setNiches(request.niches());
         if (request.referenceUrls() != null) offer.setReferenceUrls(request.referenceUrls());
         if (request.keyMessage() != null) offer.setKeyMessage(trimToNull(request.keyMessage()));
         if (request.dosAndDonts() != null) offer.setDosAndDonts(trimToNull(request.dosAndDonts()));
         if (request.hashtagsMentions() != null) offer.setHashtagsMentions(trimToNull(request.hashtagsMentions()));
         if (request.usageRights() != null) offer.setUsageRights(trimToNull(request.usageRights()));
         if (request.termsAndConditions() != null) offer.setTermsAndConditions(trimToNull(request.termsAndConditions()));
         if (request.expectedOutcomes() != null) offer.setExpectedOutcomes(trimToNull(request.expectedOutcomes()));
         if (request.coverImageUrl() != null) offer.setCoverImageUrl(request.coverImageUrl());
         if (request.deadlineDate() != null) offer.setDeadlineDate(request.deadlineDate());
         if (request.locationTargetingMode() != null || request.targetCities() != null || request.targetRegion() != null || request.targetCity() != null) {
             offer.setLocationTargetingMode(nextLocationMode);
             offer.setTargetCities("cities".equals(nextLocationMode) ? nextTargetCities : null);
             offer.setTargetRegion("region".equals(nextLocationMode) ? nextTargetRegion : null);
             offer.setTargetCity(deriveLegacyTargetCity(nextLocationMode, nextTargetCities, nextTargetRegion, request.targetCity()));
         }
         if (request.targetLanguage() != null) offer.setTargetLanguage(request.targetLanguage());
         if (request.visibility() != null) offer.setVisibility(normalizeVisibility(request.visibility()));
         if (request.creatorType() != null) offer.setCreatorType(request.creatorType());
         if (request.followerRange() != null) offer.setFollowerRange(request.followerRange());
         if (request.creatorGenderPreference() != null) offer.setCreatorGenderPreference(request.creatorGenderPreference());
         if (request.minAge() != null) offer.setMinAge(request.minAge());
         if (request.maxAge() != null) offer.setMaxAge(request.maxAge());
         if (request.applicationType() != null) offer.setApplicationType(request.applicationType());
         if (request.maxApplicants() != null) offer.setMaxApplicants(request.maxApplicants());
         if (request.proposalRequired() != null) offer.setProposalRequired(request.proposalRequired());
         if (request.portfolioRequired() != null) offer.setPortfolioRequired(request.portfolioRequired());
         if (request.customScreeningQuestions() != null) offer.setCustomScreeningQuestions(request.customScreeningQuestions());
         if (request.contentSubmissionDeadline() != null) offer.setContentSubmissionDeadline(request.contentSubmissionDeadline());
         if (request.goLiveDate() != null) offer.setGoLiveDate(request.goLiveDate());
         if (request.campaignDuration() != null) offer.setCampaignDuration(request.campaignDuration());

        return toOfferResponse(brandOfferRepository.save(offer));
    }

    @Transactional
    public BrandOfferResponse updateOfferStatus(UUID offerId, UUID brandId, UserRole role,
                                                BrandOfferStatusUpdateRequest request) {
        requireBrand(role);
        BrandOffer offer = getOwnedOffer(offerId, brandId);
        BrandOfferStatus next = parseStatus(request.status());
        BrandOfferStatus current = offer.getStatus();
        if (current == next) return toOfferResponse(offer);

        switch (next) {
            case DRAFT -> throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Cannot move back to DRAFT");
            case PUBLISHED -> {
                if (current != BrandOfferStatus.DRAFT && current != BrandOfferStatus.PAUSED) {
                    throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Only draft or paused offers can be published");
                }
                offer.setPublishedAt(Instant.now());
                offer.setClosedAt(null);
            }
            case PAUSED -> {
                if (current != BrandOfferStatus.PUBLISHED) {
                    throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Only published offers can be paused");
                }
            }
            case CLOSED -> {
                if (current != BrandOfferStatus.PUBLISHED && current != BrandOfferStatus.PAUSED) {
                    throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Only published or paused offers can be closed");
                }
                offer.setClosedAt(Instant.now());
            }
            case ARCHIVED -> {
                if (current != BrandOfferStatus.CLOSED) {
                    throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Only closed offers can be archived");
                }
            }
        }

        offer.setStatus(next);
        return toOfferResponse(brandOfferRepository.save(offer));
    }

    // ── Brand: listing ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<BrandOfferResponse> listBrandOffers(UUID brandId, UserRole role, int page, int size) {
        requireBrand(role);
        return PageResponse.from(
                brandOfferRepository.findByBrandIdOrderByCreatedAtDesc(brandId, safePage(page, size))
                        .map(this::toOfferResponse)
        );
    }

    @Transactional(readOnly = true)
    public BrandOfferResponse getBrandOffer(UUID offerId, UUID brandId, UserRole role) {
        requireBrand(role);
        return toOfferResponse(getOwnedOffer(offerId, brandId));
    }

    // ── Brand: reaction inbox ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<BrandOfferReactionResponse> listOfferReactions(UUID offerId, UUID brandId, UserRole role,
                                                                        String status, String reactionType,
                                                                        int page, int size) {
        requireBrand(role);
        getOwnedOffer(offerId, brandId);
        return PageResponse.from(
                brandOfferReactionRepository.findByOfferIdWithFilters(
                        offerId,
                        status != null ? BrandOfferReactionStatus.valueOf(status.trim().toUpperCase()) : null,
                        reactionType != null ? BrandOfferReactionType.valueOf(reactionType.trim().toUpperCase()) : null,
                        safePage(page, size))
                        .map(this::toReactionResponse)
        );
    }

    @Transactional
    public BrandOfferReactionResponse actionReaction(UUID offerId, UUID reactionId, UUID brandId,
                                                     UserRole role, BrandOfferReactionActionRequest request) {
        requireBrand(role);
        BrandOffer offer = getOwnedOffer(offerId, brandId);

        BrandOfferReaction reaction = brandOfferReactionRepository.findById(reactionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reaction not found"));
        if (!reaction.getOffer().getId().equals(offerId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Reaction does not belong to this offer");
        }
        if (reaction.getStatus() == BrandOfferReactionStatus.ACCEPTED
                || reaction.getStatus() == BrandOfferReactionStatus.REJECTED
                || reaction.getStatus() == BrandOfferReactionStatus.WITHDRAWN) {
            throw new ApiException(HttpStatus.CONFLICT, "Finalized reactions cannot be updated");
        }

        BrandOfferReactionStatus targetStatus = switch (request.action().trim().toUpperCase(Locale.ROOT)) {
            case "SHORTLIST"           -> BrandOfferReactionStatus.SHORTLISTED;
            case "REVIEW", "IN_REVIEW" -> BrandOfferReactionStatus.IN_REVIEW;
            case "ACCEPT", "ACCEPTED"  -> BrandOfferReactionStatus.ACCEPTED;
            case "REJECT", "REJECTED"  -> BrandOfferReactionStatus.REJECTED;
            default -> throw new ApiException(HttpStatus.BAD_REQUEST,
                    "action must be SHORTLIST, REVIEW, ACCEPT, or REJECT");
        };
        if (targetStatus == BrandOfferReactionStatus.ACCEPTED
                && reaction.getReactionType() != BrandOfferReactionType.PROPOSAL
                && reaction.getReactionType() != BrandOfferReactionType.INTERESTED) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Only interested or proposal responses can be accepted into an order");
        }

        reaction.setStatus(targetStatus);
        if (request.brandNote() != null) reaction.setBrandNote(request.brandNote());

        BrandOfferReaction saved = brandOfferReactionRepository.save(reaction);
        UUID orderId = null;
        if (targetStatus == BrandOfferReactionStatus.ACCEPTED) {
            ServicePackage offerPackage = createAcceptedOfferPackage(saved);
            orderId = orderService.createPrivateDealOrder(
                    offerPackage.getId(),
                    offer.getBrand().getId(),
                    effectiveAmount(saved, offer),
                    effectiveBarterDetails(offer),
                    acceptedOfferMessage(saved, offer),
                    effectiveDealType(offer)
            ).id();
        }
        notifyCreatorOfReactionAction(saved, offer, targetStatus, request.brandNote());
        return toReactionResponse(saved, orderId);
    }

    // ── Creator: discovery feed ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<BrandOfferResponse> listCreatorFeed(UserRole role,
                                                             String search, String city, String offerType, String platform, String campaignGoal,
                                                             Integer budgetMin, Integer budgetMax,
                                                             int page, int size) {
        requireCreator(role);
        return PageResponse.from(
                brandOfferRepository.findPublishedForCreatorFeed(
                        trimToNull(search), trimToNull(city), trimToNull(offerType),
                        trimToNull(platform), trimToNull(campaignGoal),
                        budgetMin, budgetMax, LocalDate.now(), safePage(page, size))
                        .map(this::toOfferResponse)
        );
    }

    @Transactional(readOnly = true)
    public BrandOfferResponse getCreatorOffer(UUID offerId, UserRole role) {
        requireCreator(role);
        BrandOffer offer = brandOfferRepository.findById(offerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Offer not found"));
        if (offer.getStatus() != BrandOfferStatus.PUBLISHED) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Offer not found");
        }
        if (offer.getDeadlineDate() != null && offer.getDeadlineDate().isBefore(LocalDate.now())) {
            throw new ApiException(HttpStatus.GONE, "Offer is expired");
        }
        return toOfferResponse(offer);
    }

    // ── Creator: react ────────────────────────────────────────────────────────

    @Transactional
    public BrandOfferReactionResponse reactToOffer(UUID offerId, UUID creatorId, UserRole role,
                                                   BrandOfferReactionCreateRequest request) {
        requireCreator(role);
        BrandOffer offer = brandOfferRepository.findById(offerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Offer not found"));
        if (offer.getStatus() != BrandOfferStatus.PUBLISHED) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "This offer is not accepting reactions");
        }
        if (offer.getDeadlineDate() != null && offer.getDeadlineDate().isBefore(LocalDate.now())) {
            throw new ApiException(HttpStatus.GONE, "Offer deadline has passed");
        }

        Creator creator = creatorRepository.findById(creatorId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator profile not found"));

        BrandOfferReactionType reactionType = parseReactionType(request.reactionType());
        validateReactionPayload(reactionType, request.proposedPrice(), request.proposedDeliveryDays(), request.message());

        BrandOfferReaction reaction = brandOfferReactionRepository
                .findByOfferIdAndCreatorId(offerId, creatorId)
                .orElse(BrandOfferReaction.builder().offer(offer).creator(creator).build());

        reaction.setReactionType(reactionType);
        reaction.setStatus(BrandOfferReactionStatus.SUBMITTED);
        reaction.setMessage(request.message());
        reaction.setProposedPrice(request.proposedPrice());
        reaction.setProposedCurrency(normalizeCurrency(request.proposedCurrency()));
        reaction.setProposedDeliveryDays(request.proposedDeliveryDays());
        reaction.setCreatorNote(request.creatorNote());

        BrandOfferReaction saved = brandOfferReactionRepository.save(reaction);

        String notifTitle = creator.getName() + " reacted to your offer";
        String notifBody = "\"" + offer.getTitle() + "\" — " + reactionType.name().toLowerCase(Locale.ROOT)
                + (request.message() != null && !request.message().isBlank()
                ? ": " + truncate(request.message(), 120) : "");
        notificationService.send(offer.getBrand().getId(), "OFFER_REACTION", notifTitle, notifBody,
                "BRAND_OFFER", offer.getId());

        return toReactionResponse(saved);
    }

    @Transactional
    public BrandOfferReactionResponse updateCreatorReaction(UUID offerId, UUID reactionId, UUID creatorId,
                                                            UserRole role, BrandOfferReactionUpdateRequest request) {
        requireCreator(role);
        BrandOfferReaction reaction = brandOfferReactionRepository.findById(reactionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reaction not found"));
        if (!reaction.getOffer().getId().equals(offerId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Reaction does not belong to this offer");
        }
        if (!reaction.getCreator().getId().equals(creatorId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can update only your own reaction");
        }
        if (reaction.getStatus() == BrandOfferReactionStatus.ACCEPTED
                || reaction.getStatus() == BrandOfferReactionStatus.REJECTED) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Finalized reactions cannot be edited");
        }

        if (request.message() != null) reaction.setMessage(request.message());
        if (request.proposedPrice() != null) reaction.setProposedPrice(request.proposedPrice());
        if (request.proposedCurrency() != null) reaction.setProposedCurrency(normalizeCurrency(request.proposedCurrency()));
        if (request.proposedDeliveryDays() != null) reaction.setProposedDeliveryDays(request.proposedDeliveryDays());
        if (request.creatorNote() != null) reaction.setCreatorNote(request.creatorNote());

        if (request.status() != null && !request.status().isBlank()) {
            BrandOfferReactionStatus next = parseReactionStatus(request.status());
            if (next != BrandOfferReactionStatus.WITHDRAWN && next != BrandOfferReactionStatus.SUBMITTED) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Creator can only set status to submitted or withdrawn");
            }
            reaction.setStatus(next);
        }

        return toReactionResponse(brandOfferReactionRepository.save(reaction));
    }

    @Transactional(readOnly = true)
    public PageResponse<BrandOfferReactionResponse> listCreatorReactions(UUID creatorId, UserRole role,
                                                                          int page, int size) {
        requireCreator(role);
        return PageResponse.from(
                brandOfferReactionRepository.findByCreatorIdWithOffer(creatorId, safePage(page, size))
                        .map(this::toReactionResponse)
        );
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private BrandOffer getOwnedOffer(UUID offerId, UUID brandId) {
        BrandOffer offer = brandOfferRepository.findById(offerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Offer not found"));
        if (!offer.getBrand().getId().equals(brandId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not own this offer");
        }
        return offer;
    }

    private void notifyCreatorOfReactionAction(BrandOfferReaction reaction, BrandOffer offer,
                                               BrandOfferReactionStatus status, String brandNote) {
        String title = switch (status) {
            case SHORTLISTED -> offer.getBrand().getDisplayName() + " shortlisted your pitch";
            case IN_REVIEW   -> offer.getBrand().getDisplayName() + " is reviewing your proposal";
            case ACCEPTED    -> "\uD83C\uDF89 " + offer.getBrand().getDisplayName() + " accepted your proposal!";
            case REJECTED    -> offer.getBrand().getDisplayName() + " passed on your pitch";
            default          -> offer.getBrand().getDisplayName() + " updated your reaction";
        };
        String body = "Offer: \"" + offer.getTitle() + "\""
                + (brandNote != null && !brandNote.isBlank() ? " — Note: " + truncate(brandNote, 120) : "");
        notificationService.send(reaction.getCreator().getId(), "OFFER_REACTION_UPDATE",
                title, body, "BRAND_OFFER_REACTION", reaction.getId());
    }

    private Pageable safePage(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }

    private BrandOfferStatus parseStatus(String value) {
        try { return BrandOfferStatus.valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (Exception ex) { throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid offer status: " + value); }
    }

    private BrandOfferReactionType parseReactionType(String value) {
        try { return BrandOfferReactionType.valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (Exception ex) { throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid reaction type: " + value); }
    }

    private BrandOfferReactionStatus parseReactionStatus(String value) {
        try { return BrandOfferReactionStatus.valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (Exception ex) { throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid reaction status: " + value); }
    }

    private void requireBrand(UserRole role) {
        if (!role.isBrand()) throw new ApiException(HttpStatus.FORBIDDEN, "Only brands can perform this action");
    }

    private void requireCreator(UserRole role) {
        if (!role.isCreator()) throw new ApiException(HttpStatus.FORBIDDEN, "Only creators can perform this action");
    }

    private void validateBudget(Integer min, Integer max) {
        if (min == null || max == null || min < 0 || max < 0 || min > max) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "budgetMin and budgetMax must be valid and budgetMin <= budgetMax");
        }
    }

    private void validateBudgetForType(String budgetType, Integer min, Integer max) {
        // Barter-only offers don't need a monetary budget
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
        // Barter-only offers don't have a cash payment structure
        if ("barter_only".equals(budgetType)) return null;
        if (value == null) return "full_upfront";
        return "split_50_50".equals(value.trim().toLowerCase(Locale.ROOT)) ? "split_50_50" : "full_upfront";
    }

    private void validateReactionPayload(BrandOfferReactionType type, Integer proposedPrice,
                                         Integer proposedDeliveryDays, String message) {
        if (type == BrandOfferReactionType.PROPOSAL) {
            if (proposedPrice == null || proposedPrice <= 0)
                throw new ApiException(HttpStatus.BAD_REQUEST, "proposedPrice is required for PROPOSAL");
            if (proposedDeliveryDays == null || proposedDeliveryDays <= 0)
                throw new ApiException(HttpStatus.BAD_REQUEST, "proposedDeliveryDays is required for PROPOSAL");
        }
        if ((type == BrandOfferReactionType.QUESTION || type == BrandOfferReactionType.DECLINE)
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
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "\u2026";
    }

    private ServicePackage createAcceptedOfferPackage(BrandOfferReaction reaction) {
        BrandOffer offer = reaction.getOffer();
        Creator creator = reaction.getCreator();
        String packageName = "brand-offer-" + reaction.getId();

        return servicePackageRepository.save(ServicePackage.builder()
                .creator(creator)
                .name(packageName)
                .title(truncate(offer.getTitle(), 200))
                .shortDescription("Accepted brand offer")
                .description(offer.getBrief())
                .fullDescription(offer.getBrief())
                .platform(resolvePlatform(offer.getTargetPlatforms()))
                .category(trimToNull(offer.getCategories()) != null ? truncate(offer.getCategories(), 80) : "Brand Offer")
                .type(PackageType.ONE_TIME)
                .dealType(effectiveDealType(offer))
                .status(PackageStatus.ACTIVE)
                .visibility("private")
                .price(effectiveAmount(reaction, offer))
                .barterDetails(effectiveBarterDetails(offer))
                .barterDescription(effectiveBarterDetails(offer))
                .estimatedBarterValue(offer.getBarterEstimatedValue())
                .hybridCashAmount(effectiveDealType(offer) == DealType.HYBRID ? effectiveAmount(reaction, offer) : null)
                .hybridBarterValue(offer.getBarterEstimatedValue())
                .creatorExpectations(reaction.getCreatorNote())
                .deliverables(parseDeliverables(offer.getDeliverables()))
                .deliveryDays(effectiveDeliveryDays(reaction, offer))
                .revisions(1)
                .tags(List.of("brand-offer"))
                .currency(normalizeCurrency(reaction.getProposedCurrency() != null
                        ? reaction.getProposedCurrency() : offer.getCurrency()))
                .responseTime("Within 24 hours")
                .active(true)
                .build());
    }

    private DealType effectiveDealType(BrandOffer offer) {
        return switch (normalizeBudgetType(offer.getBudgetType())) {
            case "barter_only" -> DealType.BARTER;
            case "paid_and_barter" -> DealType.HYBRID;
            default -> DealType.PAID;
        };
    }

    private int effectiveAmount(BrandOfferReaction reaction, BrandOffer offer) {
        if (effectiveDealType(offer) == DealType.BARTER) return 0;
        if (reaction.getProposedPrice() != null && reaction.getProposedPrice() > 0) return reaction.getProposedPrice();
        if (offer.getBudgetMax() != null && offer.getBudgetMax() > 0) return offer.getBudgetMax();
        throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Accepted paid offers require a valid amount");
    }

    private String effectiveBarterDetails(BrandOffer offer) {
        return effectiveDealType(offer) == DealType.PAID ? null : trimToNull(offer.getBarterProductDesc());
    }

    private int effectiveDeliveryDays(BrandOfferReaction reaction, BrandOffer offer) {
        if (reaction.getProposedDeliveryDays() != null && reaction.getProposedDeliveryDays() > 0) {
            return reaction.getProposedDeliveryDays();
        }
        if (offer.getCampaignDuration() != null && offer.getCampaignDuration() > 0) {
            return offer.getCampaignDuration();
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

    private String acceptedOfferMessage(BrandOfferReaction reaction, BrandOffer offer) {
        String proposal = trimToNull(reaction.getMessage());
        return proposal == null ? "Accepted response for " + offer.getTitle() : proposal;
    }

     private BrandOfferResponse toOfferResponse(BrandOffer offer) {
         return new BrandOfferResponse(
                 offer.getId(),
                 offer.getBrand().getId(),
                 offer.getBrand().getDisplayName(),
                 offer.getTitle(),
                 offer.getBrief(),
                 offer.getOfferType(),
                 offer.getBudgetMin(),
                 offer.getBudgetMax(),
                 offer.getCurrency(),
                 offer.getBudgetType(),
                 offer.getPaymentStructure(),
                 offer.getBarterProductDesc(),
                 offer.getBarterEstimatedValue(),
                 offer.getTravelCostsCovered(),
                 offer.getDeliverables(),
                 offer.getContentFormats(),
                 offer.getTargetPlatforms(),
                 offer.getCampaignGoal(),
                 offer.getCategories(),
                 offer.getNiches(),
                 offer.getReferenceUrls(),
                 offer.getKeyMessage(),
                 offer.getDosAndDonts(),
                 offer.getHashtagsMentions(),
                 offer.getUsageRights(),
                 offer.getTermsAndConditions(),
                 offer.getExpectedOutcomes(),
                 offer.getCoverImageUrl(),
                 offer.getDeadlineDate(),
                 offer.getLocationTargetingMode(),
                 offer.getTargetCities(),
                 offer.getTargetRegion(),
                 offer.getTargetCity(),
                 offer.getTargetLanguage(),
                 offer.getVisibility(),
                 offer.getCreatorType(),
                 offer.getFollowerRange(),
                 offer.getCreatorGenderPreference(),
                 offer.getMinAge(),
                 offer.getMaxAge(),
                 offer.getApplicationType(),
                 offer.getMaxApplicants(),
                 offer.getProposalRequired(),
                 offer.getPortfolioRequired(),
                 offer.getCustomScreeningQuestions(),
                 offer.getContentSubmissionDeadline(),
                 offer.getGoLiveDate(),
                 offer.getCampaignDuration(),
                 offer.getStatus().name().toLowerCase(Locale.ROOT),
                 offer.getPublishedAt(),
                 offer.getClosedAt(),
                 offer.getCreatedAt(),
                 offer.getUpdatedAt(),
                 brandOfferReactionRepository.countByOfferId(offer.getId())
         );
     }

    private BrandOfferReactionResponse toReactionResponse(BrandOfferReaction reaction) {
        UUID orderId = orderRepository.findFirstByServicePackageName("brand-offer-" + reaction.getId())
                .map(order -> order.getId())
                .orElse(null);
        return toReactionResponse(reaction, orderId);
    }

    private BrandOfferReactionResponse toReactionResponse(BrandOfferReaction reaction, UUID orderId) {
        return new BrandOfferReactionResponse(
                reaction.getId(),
                reaction.getOffer().getId(),
                reaction.getOffer().getTitle(),
                reaction.getOffer().getBrand().getDisplayName(),
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
