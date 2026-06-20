package com.zingzing.backend.controller;

import com.zingzing.backend.config.security.AuthenticatedUser;
import com.zingzing.backend.entity.CampaignAlertRule;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.service.CampaignAlertService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/brand/campaigns/{campaignId}/alerts")
public class CampaignAlertController {

    private final CampaignAlertService alertService;

    public CampaignAlertController(CampaignAlertService alertService) {
        this.alertService = alertService;
    }

    public record CreateAlertRuleRequest(
            @NotBlank String type,
            @NotNull @Min(0) Integer threshold
    ) {}

    public record UpdateAlertRuleRequest(Boolean isActive) {}

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAlertRules(
            @PathVariable UUID campaignId,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        requireBrand(authUser);
        List<CampaignAlertRule> rules = alertService.getRules(campaignId, authUser.userId());
        return ResponseEntity.ok(rules.stream().map(this::toMap).toList());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createAlertRule(
            @PathVariable UUID campaignId,
            @RequestBody CreateAlertRuleRequest request,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        requireBrand(authUser);
        CampaignAlertRule rule = alertService.createRule(
                campaignId, authUser.userId(), request.type(), request.threshold());
        return ResponseEntity.status(HttpStatus.CREATED).body(toMap(rule));
    }

    @PatchMapping("/{ruleId}")
    public ResponseEntity<Map<String, Object>> updateAlertRule(
            @PathVariable UUID campaignId,
            @PathVariable UUID ruleId,
            @RequestBody UpdateAlertRuleRequest request,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        requireBrand(authUser);
        CampaignAlertRule rule = alertService.updateRule(
                campaignId, authUser.userId(), ruleId, request.isActive());
        return ResponseEntity.ok(toMap(rule));
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Map<String, Object>> deleteAlertRule(
            @PathVariable UUID campaignId,
            @PathVariable UUID ruleId,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        requireBrand(authUser);
        alertService.deleteRule(campaignId, authUser.userId(), ruleId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    private Map<String, Object> toMap(CampaignAlertRule rule) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rule.getId());
        m.put("campaignId", rule.getCampaign().getId());
        m.put("type", rule.getType());
        m.put("threshold", rule.getThreshold());
        m.put("isActive", rule.isActive());
        m.put("lastTriggeredAt", rule.getLastTriggeredAt());
        m.put("createdAt", rule.getCreatedAt());
        return m;
    }

    private void requireBrand(AuthenticatedUser authUser) {
        if (!authUser.role().isBrand()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only brands can manage campaign alerts");
        }
    }
}
