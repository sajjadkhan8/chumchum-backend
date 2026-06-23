package com.zingzing.backend.service;

import com.zingzing.backend.entity.BrandCampaign;
import com.zingzing.backend.entity.enums.BrandCampaignStatus;
import com.zingzing.backend.repository.BrandCampaignRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class CampaignAlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(CampaignAlertScheduler.class);

    private final BrandCampaignService brandCampaignService;
    private final BrandCampaignRepository brandCampaignRepository;

    public CampaignAlertScheduler(BrandCampaignService brandCampaignService,
                                  BrandCampaignRepository brandCampaignRepository) {
        this.brandCampaignService = brandCampaignService;
        this.brandCampaignRepository = brandCampaignRepository;
    }

    // Runs hourly on the hour
    @Scheduled(cron = "0 0 * * * *")
    public void evaluateActiveCampaignAlerts() {
        List<BrandCampaign> campaigns = brandCampaignRepository.findByStatus(BrandCampaignStatus.PUBLISHED);
        log.info("Campaign alert sweep: evaluating {} published campaigns", campaigns.size());
        int success = 0;
        for (BrandCampaign campaign : campaigns) {
            UUID campaignId = campaign.getId();
            try {
                brandCampaignService.evaluateAlertRules(campaignId);
                success++;
            } catch (Exception e) {
                log.warn("Failed to evaluate alert rules for campaign {}: {}", campaignId, e.getMessage());
            }
        }
        log.info("Campaign alert sweep complete: {}/{} succeeded", success, campaigns.size());
    }
}
