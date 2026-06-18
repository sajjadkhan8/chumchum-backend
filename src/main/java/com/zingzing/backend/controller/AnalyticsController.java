package com.zingzing.backend.controller;

import com.zingzing.backend.config.security.AuthenticatedUser;
import com.zingzing.backend.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/creator/dashboard")
    public ResponseEntity<Map<String, Object>> creatorDashboard(
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        AnalyticsService.CreatorDashboard dash = analyticsService.creatorDashboard(authUser.userId(), authUser.role());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalOrders", dash.totalOrders());
        data.put("activeOrders", dash.activeOrders());
        data.put("completedOrders", dash.completedOrders());
        data.put("totalEarnings", dash.totalEarnings());
        data.put("avgRating", dash.avgRating());
        data.put("totalReviews", dash.totalReviews());
        data.put("repeatBrands", dash.repeatBrands());
        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    @GetMapping("/brand/dashboard")
    public ResponseEntity<Map<String, Object>> brandDashboard(
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        AnalyticsService.BrandDashboard dash = analyticsService.brandDashboard(authUser.userId(), authUser.role());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalOrders", dash.totalOrders());
        data.put("activeOrders", dash.activeOrders());
        data.put("completedOrders", dash.completedOrders());
        data.put("savedCreators", dash.savedCreators());
        data.put("totalSpent", dash.totalSpent());
        data.put("creatorsWorkedWith", dash.creatorsWorkedWith());
        data.put("avgRating", dash.avgRating());
        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    @GetMapping("/creator/insights")
    public ResponseEntity<Map<String, Object>> creatorInsights(
            @RequestParam(required = false) String period,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        return ResponseEntity.ok(Map.of("success", true, "data",
                analyticsService.creatorInsights(authUser.userId(), authUser.role(), period)));
    }

    @GetMapping("/creator/performance")
    public ResponseEntity<Map<String, Object>> creatorPerformance(
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        return ResponseEntity.ok(Map.of("success", true, "data",
                analyticsService.creatorPerformance(authUser.userId(), authUser.role())));
    }

    @GetMapping("/brand/campaigns")
    public ResponseEntity<Map<String, Object>> brandCampaigns(
            @RequestParam(required = false) String period,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        return ResponseEntity.ok(Map.of("success", true, "data",
                analyticsService.brandCampaigns(authUser.userId(), authUser.role(), period)));
    }
}
