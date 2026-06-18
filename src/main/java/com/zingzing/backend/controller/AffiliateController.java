package com.zingzing.backend.controller;

import com.zingzing.backend.config.security.AuthenticatedUser;
import com.zingzing.backend.dto.affiliate.AffiliateCommissionPageResponse;
import com.zingzing.backend.dto.affiliate.AffiliateOverviewResponse;
import com.zingzing.backend.service.AffiliateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/affiliate")
public class AffiliateController {

    private final AffiliateService affiliateService;

    public AffiliateController(AffiliateService affiliateService) {
        this.affiliateService = affiliateService;
    }

    @GetMapping("/me")
    public ResponseEntity<AffiliateOverviewResponse> getOverview(@AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.ok(affiliateService.getOverview(authUser.userId(), authUser.role()));
    }

    @PostMapping("/link")
    public ResponseEntity<AffiliateOverviewResponse> createOrGetLink(@AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.ok(affiliateService.createOrGetLink(authUser.userId(), authUser.role()));
    }

    @GetMapping("/commissions")
    public ResponseEntity<AffiliateCommissionPageResponse> getCommissions(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(affiliateService.getCommissions(authUser.userId(), authUser.role(), page, limit));
    }
}
