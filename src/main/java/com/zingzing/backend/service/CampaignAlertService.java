package com.zingzing.backend.service;

import com.zingzing.backend.entity.BrandCampaign;
import com.zingzing.backend.entity.CampaignAlertRule;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.repository.BrandCampaignRepository;
import com.zingzing.backend.repository.CampaignAlertRuleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CampaignAlertService {

    private static final int MAX_RULES_PER_CAMPAIGN = 10;

    private final CampaignAlertRuleRepository alertRuleRepository;
    private final BrandCampaignRepository campaignRepository;

    public CampaignAlertService(CampaignAlertRuleRepository alertRuleRepository,
                                BrandCampaignRepository campaignRepository) {
        this.alertRuleRepository = alertRuleRepository;
        this.campaignRepository = campaignRepository;
    }

    public List<CampaignAlertRule> getRules(UUID campaignId, UUID brandId) {
        requireCampaignOwnership(campaignId, brandId);
        return alertRuleRepository.findByCampaignIdOrderByCreatedAtDesc(campaignId);
    }

    @Transactional
    public CampaignAlertRule createRule(UUID campaignId, UUID brandId, String type, int threshold) {
        BrandCampaign campaign = requireCampaignOwnership(campaignId, brandId);
        if (type == null || type.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "type is required");
        }
        List<CampaignAlertRule> existing = alertRuleRepository.findByCampaignIdOrderByCreatedAtDesc(campaignId);
        if (existing.size() >= MAX_RULES_PER_CAMPAIGN) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Maximum of " + MAX_RULES_PER_CAMPAIGN + " alert rules per campaign");
        }
        CampaignAlertRule rule = CampaignAlertRule.builder()
                .campaign(campaign)
                .type(type.toLowerCase())
                .threshold(threshold)
                .active(true)
                .build();
        return alertRuleRepository.save(rule);
    }

    @Transactional
    public CampaignAlertRule updateRule(UUID campaignId, UUID brandId, UUID ruleId, Boolean isActive) {
        requireCampaignOwnership(campaignId, brandId);
        CampaignAlertRule rule = alertRuleRepository.findByIdAndCampaignId(ruleId, campaignId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Alert rule not found"));
        if (isActive != null) rule.setActive(isActive);
        return alertRuleRepository.save(rule);
    }

    @Transactional
    public void deleteRule(UUID campaignId, UUID brandId, UUID ruleId) {
        requireCampaignOwnership(campaignId, brandId);
        CampaignAlertRule rule = alertRuleRepository.findByIdAndCampaignId(ruleId, campaignId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Alert rule not found"));
        alertRuleRepository.delete(rule);
    }

    private BrandCampaign requireCampaignOwnership(UUID campaignId, UUID brandId) {
        BrandCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Campaign not found"));
        if (!campaign.getBrand().getId().equals(brandId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return campaign;
    }
}
