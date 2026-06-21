package com.zingzing.backend.repository;

import com.zingzing.backend.entity.CampaignAlertRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignAlertRuleRepository extends JpaRepository<CampaignAlertRule, UUID> {
    List<CampaignAlertRule> findByCampaignIdOrderByCreatedAtDesc(UUID campaignId);
    List<CampaignAlertRule> findByCampaignIdAndActiveTrue(UUID campaignId);
    Optional<CampaignAlertRule> findByIdAndCampaignId(UUID id, UUID campaignId);
}
