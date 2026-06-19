package com.zingzing.backend.controller;

import com.zingzing.backend.config.security.AuthenticatedUser;
import com.zingzing.backend.dto.campaign.BrandCampaignCreateRequest;
import com.zingzing.backend.dto.campaign.BrandCampaignReactionActionRequest;
import com.zingzing.backend.dto.campaign.BrandCampaignReactionCreateRequest;
import com.zingzing.backend.dto.campaign.BrandCampaignReactionResponse;
import com.zingzing.backend.dto.campaign.BrandCampaignReactionUpdateRequest;
import com.zingzing.backend.dto.campaign.BrandCampaignResponse;
import com.zingzing.backend.dto.campaign.BrandCampaignStatusUpdateRequest;
import com.zingzing.backend.dto.campaign.BrandCampaignUpdateRequest;
import com.zingzing.backend.service.BrandCampaignService;
import com.zingzing.backend.util.PageResponse;
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
public class BrandCampaignController {

    private final BrandCampaignService brandCampaignService;

    public BrandCampaignController(BrandCampaignService brandCampaignService) {
        this.brandCampaignService = brandCampaignService;
    }

    // ── Brand ─────────────────────────────────────────────────────────────────

    @PostMapping("/api/v1/brand/campaigns")
    public ResponseEntity<BrandCampaignResponse> createCampaign(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody BrandCampaignCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(brandCampaignService.createCampaign(authUser.userId(), authUser.role(), request));
    }

    @PatchMapping("/api/v1/brand/campaigns/{campaignId}")
    public ResponseEntity<BrandCampaignResponse> updateCampaign(
            @PathVariable UUID campaignId,
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody BrandCampaignUpdateRequest request
    ) {
        return ResponseEntity.ok(brandCampaignService.updateCampaign(campaignId, authUser.userId(), authUser.role(), request));
    }

    @PatchMapping("/api/v1/brand/campaigns/{campaignId}/status")
    public ResponseEntity<BrandCampaignResponse> updateStatus(
            @PathVariable UUID campaignId,
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody BrandCampaignStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(brandCampaignService.updateCampaignStatus(campaignId, authUser.userId(), authUser.role(), request));
    }

    @GetMapping("/api/v1/brand/campaigns")
    public ResponseEntity<PageResponse<BrandCampaignResponse>> listBrandCampaigns(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(brandCampaignService.listBrandCampaigns(authUser.userId(), authUser.role(), page, size, status));
    }

    @GetMapping("/api/v1/brand/campaigns/{campaignId}")
    public ResponseEntity<BrandCampaignResponse> getBrandCampaign(
            @PathVariable UUID campaignId,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        return ResponseEntity.ok(brandCampaignService.getBrandCampaign(campaignId, authUser.userId(), authUser.role()));
    }

    @GetMapping("/api/v1/brand/campaigns/{campaignId}/reactions")
    public ResponseEntity<PageResponse<BrandCampaignReactionResponse>> listCampaignReactions(
            @PathVariable UUID campaignId,
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String reactionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(brandCampaignService.listCampaignReactions(
                campaignId, authUser.userId(), authUser.role(), status, reactionType, page, size));
    }

    @PatchMapping("/api/v1/brand/campaigns/{campaignId}/reactions/{reactionId}")
    public ResponseEntity<BrandCampaignReactionResponse> actionReaction(
            @PathVariable UUID campaignId,
            @PathVariable UUID reactionId,
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody BrandCampaignReactionActionRequest request
    ) {
        return ResponseEntity.ok(brandCampaignService.actionReaction(
                campaignId, reactionId, authUser.userId(), authUser.role(), request));
    }

    // ── Creator ───────────────────────────────────────────────────────────────

    @GetMapping("/api/v1/creator/campaigns")
    public ResponseEntity<PageResponse<BrandCampaignResponse>> listCreatorFeed(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String offerType,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String campaignGoal,
            @RequestParam(required = false) Integer budgetMin,
            @RequestParam(required = false) Integer budgetMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(brandCampaignService.listCreatorFeed(
                authUser.role(), search, city, offerType, platform, campaignGoal, budgetMin, budgetMax, page, size));
    }

    @GetMapping("/api/v1/creator/campaigns/{campaignId}")
    public ResponseEntity<BrandCampaignResponse> getCreatorCampaign(
            @PathVariable UUID campaignId,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        return ResponseEntity.ok(brandCampaignService.getCreatorCampaign(campaignId, authUser.role()));
    }

    @PostMapping("/api/v1/creator/campaigns/{campaignId}/reactions")
    public ResponseEntity<BrandCampaignReactionResponse> reactToCampaign(
            @PathVariable UUID campaignId,
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody BrandCampaignReactionCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(brandCampaignService.reactToCampaign(campaignId, authUser.userId(), authUser.role(), request));
    }

    @PatchMapping("/api/v1/creator/campaigns/{campaignId}/reactions/{reactionId}")
    public ResponseEntity<BrandCampaignReactionResponse> updateCreatorReaction(
            @PathVariable UUID campaignId,
            @PathVariable UUID reactionId,
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody BrandCampaignReactionUpdateRequest request
    ) {
        return ResponseEntity.ok(brandCampaignService.updateCreatorReaction(
                campaignId, reactionId, authUser.userId(), authUser.role(), request));
    }

    @GetMapping("/api/v1/creator/campaigns/reactions/mine")
    public ResponseEntity<PageResponse<BrandCampaignReactionResponse>> listMyReactions(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(brandCampaignService.listCreatorReactions(authUser.userId(), authUser.role(), page, size));
    }
}
