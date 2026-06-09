package com.chamcham.backend.controller;

import com.chamcham.backend.config.security.AuthenticatedUser;
import com.chamcham.backend.dto.offer.BrandOfferCreateRequest;
import com.chamcham.backend.dto.offer.BrandOfferReactionActionRequest;
import com.chamcham.backend.dto.offer.BrandOfferReactionCreateRequest;
import com.chamcham.backend.dto.offer.BrandOfferReactionResponse;
import com.chamcham.backend.dto.offer.BrandOfferReactionUpdateRequest;
import com.chamcham.backend.dto.offer.BrandOfferResponse;
import com.chamcham.backend.dto.offer.BrandOfferStatusUpdateRequest;
import com.chamcham.backend.dto.offer.BrandOfferUpdateRequest;
import com.chamcham.backend.service.BrandOfferService;
import com.chamcham.backend.util.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class BrandOfferController {

    private final BrandOfferService brandOfferService;

    public BrandOfferController(BrandOfferService brandOfferService) {
        this.brandOfferService = brandOfferService;
    }

    // ── Brand ─────────────────────────────────────────────────────────────────

    @PostMapping("/api/v1/brand/offers")
    public ResponseEntity<BrandOfferResponse> createOffer(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody BrandOfferCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(brandOfferService.createOffer(authUser.userId(), authUser.role(), request));
    }

    @PatchMapping("/api/v1/brand/offers/{offerId}")
    public ResponseEntity<BrandOfferResponse> updateOffer(
            @PathVariable UUID offerId,
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody BrandOfferUpdateRequest request
    ) {
        return ResponseEntity.ok(brandOfferService.updateOffer(offerId, authUser.userId(), authUser.role(), request));
    }

    @PatchMapping("/api/v1/brand/offers/{offerId}/status")
    public ResponseEntity<BrandOfferResponse> updateStatus(
            @PathVariable UUID offerId,
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody BrandOfferStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(brandOfferService.updateOfferStatus(offerId, authUser.userId(), authUser.role(), request));
    }

    @GetMapping("/api/v1/brand/offers")
    public ResponseEntity<PageResponse<BrandOfferResponse>> listBrandOffers(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(brandOfferService.listBrandOffers(authUser.userId(), authUser.role(), page, size));
    }

    @GetMapping("/api/v1/brand/offers/{offerId}")
    public ResponseEntity<BrandOfferResponse> getBrandOffer(
            @PathVariable UUID offerId,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        return ResponseEntity.ok(brandOfferService.getBrandOffer(offerId, authUser.userId(), authUser.role()));
    }

    @GetMapping("/api/v1/brand/offers/{offerId}/reactions")
    public ResponseEntity<PageResponse<BrandOfferReactionResponse>> listOfferReactions(
            @PathVariable UUID offerId,
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String reactionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(brandOfferService.listOfferReactions(
                offerId, authUser.userId(), authUser.role(), status, reactionType, page, size));
    }

    @PatchMapping("/api/v1/brand/offers/{offerId}/reactions/{reactionId}")
    public ResponseEntity<BrandOfferReactionResponse> actionReaction(
            @PathVariable UUID offerId,
            @PathVariable UUID reactionId,
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody BrandOfferReactionActionRequest request
    ) {
        return ResponseEntity.ok(brandOfferService.actionReaction(
                offerId, reactionId, authUser.userId(), authUser.role(), request));
    }

    // ── Creator ───────────────────────────────────────────────────────────────

    @GetMapping("/api/v1/creator/offers")
    public ResponseEntity<PageResponse<BrandOfferResponse>> listCreatorFeed(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String offerType,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String campaignGoal,
            @RequestParam(required = false) Integer budgetMin,
            @RequestParam(required = false) Integer budgetMax,
            @RequestParam(required = false) Integer myFollowers,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(brandOfferService.listCreatorFeed(
                authUser.role(), search, city, offerType, platform, campaignGoal, budgetMin, budgetMax, myFollowers, page, size));
    }

    @GetMapping("/api/v1/creator/offers/{offerId}")
    public ResponseEntity<BrandOfferResponse> getCreatorOffer(
            @PathVariable UUID offerId,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        return ResponseEntity.ok(brandOfferService.getCreatorOffer(offerId, authUser.role()));
    }

    @PostMapping("/api/v1/creator/offers/{offerId}/reactions")
    public ResponseEntity<BrandOfferReactionResponse> reactToOffer(
            @PathVariable UUID offerId,
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody BrandOfferReactionCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(brandOfferService.reactToOffer(offerId, authUser.userId(), authUser.role(), request));
    }

    @PatchMapping("/api/v1/creator/offers/{offerId}/reactions/{reactionId}")
    public ResponseEntity<BrandOfferReactionResponse> updateCreatorReaction(
            @PathVariable UUID offerId,
            @PathVariable UUID reactionId,
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody BrandOfferReactionUpdateRequest request
    ) {
        return ResponseEntity.ok(brandOfferService.updateCreatorReaction(
                offerId, reactionId, authUser.userId(), authUser.role(), request));
    }

    @GetMapping("/api/v1/creator/offers/reactions/mine")
    public ResponseEntity<PageResponse<BrandOfferReactionResponse>> listMyReactions(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(brandOfferService.listCreatorReactions(authUser.userId(), authUser.role(), page, size));
    }
}
