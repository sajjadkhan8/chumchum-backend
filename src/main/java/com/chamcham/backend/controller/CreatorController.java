package com.chamcham.backend.controller;

import com.chamcham.backend.config.security.AuthenticatedUser;
import com.chamcham.backend.dto.creator.CreatorCreateRequest;
import com.chamcham.backend.dto.creator.CreatorResponse;
import com.chamcham.backend.dto.creator.CreatorUpdateRequest;
import com.chamcham.backend.dto.review.ReviewResponse;
import com.chamcham.backend.dto.servicepackage.ServicePackageResponse;
import com.chamcham.backend.entity.SocialAccount;
import com.chamcham.backend.service.CreatorService;
import com.chamcham.backend.service.ReviewService;
import com.chamcham.backend.service.ServicePackageService;
import com.chamcham.backend.util.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/creators")
public class CreatorController {

    private final CreatorService creatorService;
    private final ReviewService reviewService;
    private final ServicePackageService packageService;

    public CreatorController(CreatorService creatorService, ReviewService reviewService,
                             ServicePackageService packageService) {
        this.creatorService = creatorService;
        this.reviewService = reviewService;
        this.packageService = packageService;
    }

    @PostMapping
    public ResponseEntity<CreatorResponse> create(
            @Valid @RequestBody CreatorCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(creatorService.create(request));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Integer minFollowers,
            @RequestParam(required = false) Integer maxFollowers,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Boolean acceptsBarter,
            @RequestParam(required = false) Boolean isTrending,
            @RequestParam(required = false) Boolean isFastResponder,
            @RequestParam(defaultValue = "false") Boolean ambassadorOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        CreatorService.CreatorSearchResult r = creatorService.search(
                search, city, minFollowers, maxFollowers, minRating, minPrice, maxPrice,
                acceptsBarter, isTrending, isFastResponder, ambassadorOnly, page, limit, sortBy);
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

    @GetMapping("/by-city")
    public ResponseEntity<Map<String, Object>> byCity(
            @RequestParam String city,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(Map.of("success", true, "data", creatorService.getByCity(city, limit)));
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
                accounts.stream().map(a -> Map.of(
                        "id", a.getId(),
                        "platform", a.getPlatform(),
                        "username", a.getUsername(),
                        "followers", a.getFollowers()
                )).toList()));
    }

    @PatchMapping("/me/preferences")
    public ResponseEntity<Map<String, Object>> updatePreferences(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestBody CreatorService.PreferencesRequest req
    ) {
        CreatorResponse updated = creatorService.updatePreferences(authUser.userId(), authUser.role(), req);
        return ResponseEntity.ok(Map.of("success", true, "data", updated));
    }

    @PatchMapping("/me/payment-settings")
    public ResponseEntity<Map<String, Object>> updatePaymentSettings(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestBody CreatorService.PaymentSettingsRequest req
    ) {
        creatorService.updatePaymentSettings(authUser.userId(), authUser.role(), req);
        return ResponseEntity.ok(Map.of("success", true, "message", "Payment settings updated"));
    }
}
