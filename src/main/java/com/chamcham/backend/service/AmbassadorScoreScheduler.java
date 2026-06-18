package com.chamcham.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class AmbassadorScoreScheduler {

    private static final Logger log = LoggerFactory.getLogger(AmbassadorScoreScheduler.class);

    private final AmbassadorService ambassadorService;
    private final com.chamcham.backend.repository.CreatorRepository creatorRepository;

    public AmbassadorScoreScheduler(AmbassadorService ambassadorService,
                                     com.chamcham.backend.repository.CreatorRepository creatorRepository) {
        this.ambassadorService = ambassadorService;
        this.creatorRepository = creatorRepository;
    }

    // Runs nightly at 01:00 UTC (06:00 PKT)
    @Scheduled(cron = "0 0 1 * * *")
    public void refreshAllScores() {
        List<UUID> creatorIds = creatorRepository.findAllIds();
        log.info("Ambassador score refresh: processing {} creators", creatorIds.size());
        int success = 0;
        for (UUID creatorId : creatorIds) {
            try {
                ambassadorService.computeAndSave(creatorId);
                success++;
            } catch (Exception e) {
                log.warn("Failed to refresh ambassador score for creator {}: {}", creatorId, e.getMessage());
            }
        }
        log.info("Ambassador score refresh complete: {}/{} succeeded", success, creatorIds.size());
    }
}
