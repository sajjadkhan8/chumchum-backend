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
import com.zingzing.backend.entity.BrandCampaign;
import com.zingzing.backend.entity.CampaignAlertRule;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.repository.BrandCampaignRepository;
import com.zingzing.backend.repository.CampaignAlertRuleRepository;
import com.zingzing.backend.service.BrandCampaignService;
import com.zingzing.backend.util.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class BrandCampaignController {

    private final BrandCampaignService brandCampaignService;
    private final BrandCampaignRepository brandCampaignRepository;
    private final CampaignAlertRuleRepository campaignAlertRuleRepository;

    public BrandCampaignController(BrandCampaignService brandCampaignService,
                                   BrandCampaignRepository brandCampaignRepository,
                                   CampaignAlertRuleRepository campaignAlertRuleRepository) {
        this.brandCampaignService = brandCampaignService;
        this.brandCampaignRepository = brandCampaignRepository;
        this.campaignAlertRuleRepository = campaignAlertRuleRepository;
    }

    public record CampaignAlertRuleRequest(@NotBlank String type, @NotNull @Min(1) Integer threshold) {}
    public record CampaignAlertRuleUpdateRequest(Boolean isActive) {}

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

    @GetMapping("/api/v1/brand/campaigns/{campaignId}/alerts")
    public ResponseEntity<Map<String, Object>> listAlertRules(
            @PathVariable UUID campaignId,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        BrandCampaign campaign = getOwnedCampaign(campaignId, authUser);
        List<Map<String, Object>> alerts = campaignAlertRuleRepository.findByCampaignIdOrderByCreatedAtDesc(campaign.getId())
                .stream()
                .map(this::toAlertMap)
                .toList();
        return ResponseEntity.ok(Map.of("success", true, "data", alerts));
    }

    @PostMapping("/api/v1/brand/campaigns/{campaignId}/alerts")
    @Transactional
    public ResponseEntity<Map<String, Object>> createAlertRule(
            @PathVariable UUID campaignId,
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody CampaignAlertRuleRequest request
    ) {
        BrandCampaign campaign = getOwnedCampaign(campaignId, authUser);
        CampaignAlertRule saved = campaignAlertRuleRepository.save(CampaignAlertRule.builder()
                .campaign(campaign)
                .type(request.type().trim())
                .threshold(request.threshold())
                .active(true)
                .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "data", toAlertMap(saved)));
    }

    @PatchMapping("/api/v1/brand/campaigns/{campaignId}/alerts/{ruleId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateAlertRule(
            @PathVariable UUID campaignId,
            @PathVariable UUID ruleId,
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestBody CampaignAlertRuleUpdateRequest request
    ) {
        getOwnedCampaign(campaignId, authUser);
        CampaignAlertRule rule = campaignAlertRuleRepository.findByIdAndCampaignId(ruleId, campaignId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Campaign alert rule not found"));
        if (request.isActive() != null) rule.setActive(request.isActive());
        return ResponseEntity.ok(Map.of("success", true, "data", toAlertMap(campaignAlertRuleRepository.save(rule))));
    }

    @DeleteMapping("/api/v1/brand/campaigns/{campaignId}/alerts/{ruleId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteAlertRule(
            @PathVariable UUID campaignId,
            @PathVariable UUID ruleId,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        getOwnedCampaign(campaignId, authUser);
        CampaignAlertRule rule = campaignAlertRuleRepository.findByIdAndCampaignId(ruleId, campaignId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Campaign alert rule not found"));
        campaignAlertRuleRepository.delete(rule);
        return ResponseEntity.ok(Map.of("success", true, "message", "Campaign alert deleted"));
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

    private BrandCampaign getOwnedCampaign(UUID campaignId, AuthenticatedUser authUser) {
        if (authUser == null || !authUser.role().isBrand()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only brands can manage campaign alerts");
        }
        BrandCampaign campaign = brandCampaignRepository.findById(campaignId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Campaign not found"));
        if (!campaign.getBrand().getId().equals(authUser.userId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Campaign does not belong to this brand");
        }
        return campaign;
    }

    private Map<String, Object> toAlertMap(CampaignAlertRule rule) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rule.getId());
        row.put("campaignId", rule.getCampaign().getId());
        row.put("type", rule.getType());
        row.put("threshold", rule.getThreshold());
        row.put("isActive", rule.isActive());
        row.put("createdAt", rule.getCreatedAt());
        row.put("lastTriggeredAt", rule.getLastTriggeredAt());
        return row;
    }
}
