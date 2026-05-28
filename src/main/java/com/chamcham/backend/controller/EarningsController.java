package com.chamcham.backend.controller;

import com.chamcham.backend.config.security.AuthenticatedUser;
import com.chamcham.backend.entity.Transaction;
import com.chamcham.backend.service.EarningsService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/earnings")
public class EarningsController {

    private final EarningsService earningsService;

    public EarningsController(EarningsService earningsService) {
        this.earningsService = earningsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        EarningsService.EarningsSummary summary = earningsService.getSummary(authUser.userId(), authUser.role());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalEarned", summary.totalEarned());
        data.put("availableBalance", summary.availableBalance());
        data.put("pendingBalance", summary.pendingBalance());
        data.put("totalWithdrawn", summary.totalWithdrawn());
        data.put("platformFees", summary.platformFees());
        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    @GetMapping("/transactions")
    public ResponseEntity<Map<String, Object>> getTransactions(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        Page<Transaction> txPage = earningsService.getTransactions(authUser.userId(), authUser.role(), page, limit);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("transactions", txPage.getContent().stream().map(tx -> {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("id", tx.getId());
            t.put("type", tx.getType().name().toLowerCase());
            t.put("amount", tx.getAmount());
            t.put("description", tx.getDescription());
            t.put("status", tx.getStatus().name().toLowerCase());
            t.put("createdAt", tx.getCreatedAt());
            return t;
        }).toList());
        data.put("total", txPage.getTotalElements());
        data.put("page", page);
        data.put("limit", limit);
        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }
}

