package com.zingzing.backend.controller;

import com.zingzing.backend.config.security.AuthenticatedUser;
import com.zingzing.backend.dto.creator.CreatorResponse;
import com.zingzing.backend.dto.creator.CreatorUpdateRequest;
import com.zingzing.backend.dto.review.ReviewResponse;
import com.zingzing.backend.dto.servicepackage.ServicePackageResponse;
import com.zingzing.backend.entity.ContentPreview;
import com.zingzing.backend.entity.Creator;
import com.zingzing.backend.entity.CreatorVerificationDocument;
import com.zingzing.backend.entity.CreatorVerificationEvent;
import com.zingzing.backend.entity.SocialAccount;
import com.zingzing.backend.entity.enums.AvailabilityStatus;
import com.zingzing.backend.entity.enums.CreatorBadgeLevel;
import com.zingzing.backend.entity.enums.UserRole;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.repository.ContentPreviewRepository;
import com.zingzing.backend.repository.CreatorRepository;
import com.zingzing.backend.repository.CreatorVerificationDocumentRepository;
import com.zingzing.backend.repository.CreatorVerificationEventRepository;
import com.zingzing.backend.service.CreatorService;
import com.zingzing.backend.service.ReviewService;
import com.zingzing.backend.service.ServicePackageService;
import com.zingzing.backend.util.CreatorVerificationStatuses;
import com.zingzing.backend.util.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/creators")
public class CreatorController {

    private final CreatorService creatorService;
    private final ReviewService reviewService;
    private final ServicePackageService packageService;
    private final CreatorRepository creatorRepository;
    private final ContentPreviewRepository contentPreviewRepository;
    private final CreatorVerificationDocumentRepository verificationDocumentRepository;
    private final CreatorVerificationEventRepository verificationEventRepository;

    public CreatorController(CreatorService creatorService, ReviewService reviewService,
                             ServicePackageService packageService,
                             CreatorRepository creatorRepository,
                             ContentPreviewRepository contentPreviewRepository,
                             CreatorVerificationDocumentRepository verificationDocumentRepository,
                             CreatorVerificationEventRepository verificationEventRepository) {
        this.creatorService = creatorService;
        this.reviewService = reviewService;
        this.packageService = packageService;
        this.creatorRepository = creatorRepository;
        this.contentPreviewRepository = contentPreviewRepository;
        this.verificationDocumentRepository = verificationDocumentRepository;
        this.verificationEventRepository = verificationEventRepository;
    }

    public record VerificationDocumentRequest(
            @NotBlank @Size(max = 40) String type,
            @NotBlank @Size(max = 600) String fileUrl,
            @NotBlank @Size(max = 255) String fileName
    ) {}

    @GetMapping
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<String> cities,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) List<String> languages,
            @RequestParam(required = false) Integer minFollowers,
            @RequestParam(required = false) Integer maxFollowers,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Integer minReviews,
            @RequestParam(required = false) CreatorBadgeLevel badgeLevel,
            @RequestParam(required = false) AvailabilityStatus availabilityStatus,
            @RequestParam(required = false) Boolean acceptsBarter,
            @RequestParam(required = false) Boolean isTrending,
            @RequestParam(required = false) Boolean isFastResponder,
            @RequestParam(defaultValue = "false") Boolean ambassadorOnly,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) BigDecimal minEngagementRate,
            @RequestParam(required = false) Integer minCompletionRate,
            @RequestParam(required = false) Integer maxRateCardReel,
            @RequestParam(required = false) Integer maxRateCardStory,
            @RequestParam(required = false) Integer maxRateCardPost,
            @RequestParam(required = false) Integer maxRateCardVideo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        CreatorService.CreatorSearchResult r = creatorService.search(
                search, cities, categories, languages,
                minFollowers, maxFollowers, minRating, minPrice, maxPrice, minReviews,
                badgeLevel, availabilityStatus, acceptsBarter, isTrending, isFastResponder,
                ambassadorOnly, platform, minEngagementRate,
                minCompletionRate, maxRateCardReel, maxRateCardStory, maxRateCardPost, maxRateCardVideo,
                page, limit, sortBy);
        return ResponseEntity.ok(Map.of("success", true, "data",
                Map.of("creators", r.creators(), "total", r.total(),
                        "page", r.page(), "limit", r.limit())));
    }

    @GetMapping("/trending")
    public ResponseEntity<Map<String, Object>> trending(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(Map.of("success", true, "data", creatorService.getTrending(limit)));
    }

    @GetMapping("/barter-friendly")
    public ResponseEntity<Map<String, Object>> barterFriendly(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(Map.of("success", true, "data", creatorService.getBarterFriendly(limit)));
    }

    @GetMapping("/fast-responders")
    public ResponseEntity<Map<String, Object>> fastResponders(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(Map.of("success", true, "data", creatorService.getFastResponders(limit)));
    }

    @GetMapping("/rising-stars")
    public ResponseEntity<Map<String, Object>> risingStars(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(Map.of("success", true, "data", creatorService.getRisingStars(limit)));
    }

    @GetMapping("/verified")
    public ResponseEntity<Map<String, Object>> verified(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(Map.of("success", true, "data", creatorService.getVerified(limit)));
    }

    @GetMapping("/by-city")
    public ResponseEntity<Map<String, Object>> byCity(
            @RequestParam String city,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(Map.of("success", true, "data", creatorService.getByCity(city, limit)));
    }

    @GetMapping("/by-username/{username}")
    public ResponseEntity<CreatorResponse> getByUsername(@PathVariable String username) {
        return ResponseEntity.ok(creatorService.getByUsername(username));
    }

    @GetMapping("/{creatorId}")
    public ResponseEntity<CreatorResponse> getById(@PathVariable UUID creatorId) {
        return ResponseEntity.ok(creatorService.getById(creatorId));
    }

    @GetMapping("/{creatorId}/reviews")
    public ResponseEntity<List<ReviewResponse>> getCreatorReviews(@PathVariable UUID creatorId) {
        return ResponseEntity.ok(reviewService.getReviewsByCreator(creatorId));
    }

    @GetMapping("/{creatorId}/packages")
    public ResponseEntity<PageResponse<ServicePackageResponse>> getCreatorPackages(
            @PathVariable UUID creatorId,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String dealType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(packageService.getPackages(null, null, null, null,
                creatorId, null, page, limit, "createdAt"));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<CreatorResponse> getByUserId(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(creatorService.getByUserId(authUser.userId(), authUser.role(), userId));
    }

    @GetMapping("/me/profile")
    public ResponseEntity<CreatorResponse> meProfile(@AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.ok(creatorService.getByUserId(authUser.userId(), authUser.role(), authUser.userId()));
    }

    @GetMapping("/me/verification-documents")
    public ResponseEntity<Map<String, Object>> verificationDocuments(
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        requireCreator(authUser);
        List<Map<String, Object>> docs = verificationDocumentRepository
                .findByCreatorIdOrderByUploadedAtDesc(authUser.userId())
                .stream()
                .map(this::toVerificationDocumentMap)
                .toList();
        return ResponseEntity.ok(Map.of("success", true, "data", docs));
    }

    @GetMapping("/me/verification-events")
    public ResponseEntity<Map<String, Object>> verificationEvents(
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        requireCreator(authUser);
        List<Map<String, Object>> events = verificationEventRepository
                .findByCreatorIdOrderByCreatedAtDesc(authUser.userId())
                .stream()
                .map(this::toVerificationEventMap)
                .toList();
        return ResponseEntity.ok(Map.of("success", true, "data", events));
    }

    @PostMapping("/me/verification-documents")
    @Transactional
    public ResponseEntity<Map<String, Object>> submitVerificationDocument(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody VerificationDocumentRequest request
    ) {
        requireCreator(authUser);
        Creator creator = creatorRepository.findById(authUser.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator profile not found"));
        CreatorVerificationDocument saved = verificationDocumentRepository.save(CreatorVerificationDocument.builder()
                .creator(creator)
                .type(request.type().trim())
                .fileUrl(request.fileUrl().trim())
                .fileName(request.fileName().trim())
                .status("PENDING")
                .build());
        String currentStatus = CreatorVerificationStatuses.normalizeForResponse(creator.getVerificationStatus());
        if (CreatorVerificationStatuses.UNVERIFIED.equals(currentStatus)) {
            creator.setVerificationStatus(CreatorVerificationStatuses.PENDING);
            creatorRepository.save(creator);
        }
        verificationEventRepository.save(CreatorVerificationEvent.builder()
                .creator(creator)
                .document(saved)
                .actor(creator)
                .eventType("DOCUMENT_UPLOADED")
                .details(request.type().trim() + ": " + request.fileName().trim())
                .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "data", toVerificationDocumentMap(saved)));
    }

    @PostMapping("/me/verification/submit")
    @Transactional
    public ResponseEntity<Map<String, Object>> submitVerificationForReview(
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        requireCreator(authUser);
        Creator creator = creatorRepository.findById(authUser.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator profile not found"));
        if (verificationDocumentRepository.findByCreatorIdOrderByUploadedAtDesc(creator.getId()).isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Upload at least one verification document before submitting");
        }
        creator.setVerificationStatus(CreatorVerificationStatuses.UNDER_REVIEW);
        creatorRepository.save(creator);
        verificationEventRepository.save(CreatorVerificationEvent.builder()
                .creator(creator)
                .actor(creator)
                .eventType("SUBMITTED_FOR_REVIEW")
                .details("Creator submitted verification documents for review")
                .build());
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("success", true)));
    }

    @PutMapping("/{creatorId}")
    public ResponseEntity<CreatorResponse> update(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID creatorId,
            @Valid @RequestBody CreatorUpdateRequest request
    ) {
        return ResponseEntity.ok(creatorService.update(authUser.userId(), authUser.role(), creatorId, request));
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<CreatorResponse> updateMe(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody CreatorUpdateRequest request
    ) {
        return ResponseEntity.ok(creatorService.update(authUser.userId(), authUser.role(), authUser.userId(), request));
    }

    @PutMapping("/me/social-accounts")
    public ResponseEntity<Map<String, Object>> updateSocialAccounts(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestBody Map<String, List<CreatorService.SocialAccountRequest>> body
    ) {
        List<SocialAccount> accounts = creatorService.updateSocialAccounts(
                authUser.userId(), authUser.role(), body.getOrDefault("accounts", List.of()));
        return ResponseEntity.ok(Map.of("success", true, "data",
                accounts.stream().map(a -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", a.getId());
                    row.put("platform", a.getPlatform());
                    row.put("username", a.getUsername());
                    row.put("profileUrl", a.getProfileUrl());
                    row.put("followers", a.getFollowers());
                    row.put("avgViews", a.getAvgViews());
                    row.put("engagementRate", a.getEngagementRate());
                    row.put("isVerified", a.isVerified());
                    return row;
                }).toList()));
    }

    @PatchMapping("/me/social-accounts/{platform}")
    public ResponseEntity<Map<String, Object>> patchSocialAccount(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable String platform,
            @RequestBody CreatorService.SocialAccountPatchRequest req
    ) {
        requireCreator(authUser);
        SocialAccount account = creatorService.patchSocialAccount(authUser.userId(), platform, req);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", account.getId());
        row.put("platform", account.getPlatform());
        row.put("username", account.getUsername());
        row.put("profileUrl", account.getProfileUrl());
        row.put("followers", account.getFollowers());
        row.put("avgViews", account.getAvgViews());
        row.put("engagementRate", account.getEngagementRate());
        row.put("isVerified", account.isVerified());
        return ResponseEntity.ok(Map.of("success", true, "data", row));
    }

    @DeleteMapping("/me/social-accounts/{platform}")
    public ResponseEntity<Map<String, Object>> deleteSocialAccount(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable String platform
    ) {
        creatorService.deleteSocialAccount(authUser.userId(), authUser.role(), platform);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PatchMapping("/me/preferences")
    public ResponseEntity<Map<String, Object>> updatePreferences(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestBody CreatorService.PreferencesRequest req
    ) {
        CreatorResponse updated = creatorService.updatePreferences(authUser.userId(), authUser.role(), req);
        return ResponseEntity.ok(Map.of("success", true, "data", updated));
    }

    @GetMapping("/me/payment-settings")
    public ResponseEntity<Map<String, Object>> getPaymentSettings(
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", creatorService.getPaymentSettings(authUser.userId(), authUser.role())
        ));
    }

    @PatchMapping("/me/payment-settings")
    public ResponseEntity<Map<String, Object>> updatePaymentSettings(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestBody CreatorService.PaymentSettingsRequest req
    ) {
        creatorService.updatePaymentSettings(authUser.userId(), authUser.role(), req);
        return ResponseEntity.ok(Map.of("success", true, "message", "Payment settings updated"));
    }

    @GetMapping("/me/payout-preferences")
    public ResponseEntity<Map<String, Object>> getPayoutPreferences(
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", creatorService.getPayoutPreferences(authUser.userId(), authUser.role())
        ));
    }

    @PatchMapping("/me/payout-preferences")
    public ResponseEntity<Map<String, Object>> updatePayoutPreferences(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestBody CreatorService.PayoutPreferencesRequest req
    ) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", creatorService.updatePayoutPreferences(authUser.userId(), authUser.role(), req)
        ));
    }

    public record PortfolioItemRequest(
            @NotBlank String type,
            @NotBlank String thumbnailUrl,
            @NotBlank String mediaUrl,
            @NotBlank String platform,
            Integer views,
            Integer likes
    ) {}

    public record PortfolioReorderRequest(List<UUID> ids) {}

    @PostMapping("/me/portfolio")
    public ResponseEntity<Map<String, Object>> addPortfolioItem(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody PortfolioItemRequest req
    ) {
        Creator creator = creatorRepository.findById(authUser.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator profile not found"));
        ContentPreview preview = ContentPreview.builder()
                .creator(creator)
                .type(req.type())
                .thumbnailUrl(req.thumbnailUrl())
                .mediaUrl(req.mediaUrl())
                .platform(req.platform())
                .views(req.views() != null ? req.views() : 0)
                .likes(req.likes() != null ? req.likes() : 0)
                .build();
        ContentPreview saved = contentPreviewRepository.save(preview);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", saved.getId());
        row.put("type", saved.getType());
        row.put("thumbnailUrl", saved.getThumbnailUrl());
        row.put("mediaUrl", saved.getMediaUrl());
        row.put("platform", saved.getPlatform());
        row.put("views", saved.getViews());
        row.put("likes", saved.getLikes());
        return ResponseEntity.ok(Map.of("success", true, "data", row));
    }

    @PutMapping("/me/portfolio/reorder")
    public ResponseEntity<Map<String, Object>> reorderPortfolioItems(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestBody PortfolioReorderRequest request
    ) {
        List<UUID> ids = request.ids() == null ? List.of() : request.ids();
        List<ContentPreview> ownItems = contentPreviewRepository.findByCreatorIdOrderByPositionAscCreatedAtDesc(authUser.userId());
        Map<UUID, ContentPreview> byId = ownItems.stream()
                .collect(java.util.stream.Collectors.toMap(ContentPreview::getId, item -> item));
        int position = 0;
        for (UUID id : ids) {
            ContentPreview item = byId.remove(id);
            if (item == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Portfolio item does not belong to this creator: " + id);
            }
            item.setPosition(position++);
            contentPreviewRepository.save(item);
        }
        for (ContentPreview item : byId.values()) {
            item.setPosition(position++);
            contentPreviewRepository.save(item);
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Portfolio order updated"));
    }

    @DeleteMapping("/me/portfolio/{itemId}")
    public ResponseEntity<Map<String, Object>> deletePortfolioItem(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID itemId
    ) {
        ContentPreview preview = contentPreviewRepository.findById(itemId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Portfolio item not found"));
        if (!preview.getCreator().getId().equals(authUser.userId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your portfolio item");
        }
        contentPreviewRepository.delete(preview);
        return ResponseEntity.ok(Map.of("success", true, "message", "Portfolio item removed"));
    }

    private void requireCreator(AuthenticatedUser authUser) {
        if (authUser == null || authUser.role() != UserRole.CREATOR) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only creators can use this endpoint");
        }
    }

    private Map<String, Object> toVerificationDocumentMap(CreatorVerificationDocument doc) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", doc.getId());
        row.put("type", doc.getType());
        row.put("fileName", doc.getFileName());
        row.put("fileUrl", doc.getFileUrl());
        row.put("status", doc.getStatus() == null ? "pending" : doc.getStatus().toLowerCase());
        row.put("rejectionReason", doc.getRejectionReason());
        row.put("uploadedAt", doc.getUploadedAt());
        row.put("reviewedAt", doc.getReviewedAt());
        return row;
    }

    private Map<String, Object> toVerificationEventMap(CreatorVerificationEvent event) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", event.getId());
        row.put("eventType", event.getEventType());
        row.put("details", event.getDetails());
        row.put("documentId", event.getDocument() == null ? null : event.getDocument().getId());
        row.put("createdAt", event.getCreatedAt());
        return row;
    }
}
