package com.chamcham.backend.controller;

import com.chamcham.backend.config.security.AuthenticatedUser;
import com.chamcham.backend.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }
}

