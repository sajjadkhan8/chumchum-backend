package com.chamcham.backend.controller;

import com.chamcham.backend.config.security.AuthenticatedUser;
import com.chamcham.backend.dto.subscription.SubscriptionResponse;
import com.chamcham.backend.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> subscribe(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam UUID packageId
    ) {
        SubscriptionResponse response = subscriptionService.subscribe(
                authUser.userId(), authUser.role(), packageId);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> mySubscriptions(
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        List<SubscriptionResponse> list = subscriptionService.getMySubscriptions(
                authUser.userId(), authUser.role());
        return ResponseEntity.ok(Map.of("success", true, "data", list));
    }

    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<Map<String, Object>> cancel(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID subscriptionId
    ) {
        SubscriptionResponse response = subscriptionService.cancel(
                authUser.userId(), authUser.role(), subscriptionId);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }
}
