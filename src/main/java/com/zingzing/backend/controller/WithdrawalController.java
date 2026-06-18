package com.zingzing.backend.controller;

import com.zingzing.backend.config.security.AuthenticatedUser;
import com.zingzing.backend.entity.WithdrawalRequest;
import com.zingzing.backend.service.WithdrawalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/withdrawals")
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    public WithdrawalController(WithdrawalService withdrawalService) {
        this.withdrawalService = withdrawalService;
    }

    public record CreateWithdrawalRequest(
            @NotNull UUID payoutMethodId,
            @NotNull @Min(1) Integer amount
    ) {}

    @PostMapping
    public ResponseEntity<Map<String, Object>> requestWithdrawal(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody CreateWithdrawalRequest req
    ) {
        WithdrawalRequest wr = withdrawalService.requestWithdrawal(
                authUser.userId(), authUser.role(), req.payoutMethodId(), req.amount());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("success", true, "data", toMap(wr)));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        Page<WithdrawalRequest> wrPage = withdrawalService.list(authUser.userId(), authUser.role(), page, limit);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("withdrawals", wrPage.getContent().stream().map(this::toMap).toList());
        data.put("total", wrPage.getTotalElements());
        data.put("page", page);
        data.put("limit", limit);
        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    private Map<String, Object> toMap(WithdrawalRequest wr) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", wr.getId());
        m.put("amount", wr.getAmount());
        m.put("status", wr.getStatus().name().toLowerCase());
        m.put("payoutMethodId", wr.getPayoutMethod().getId());
        m.put("processedAt", wr.getProcessedAt());
        m.put("createdAt", wr.getCreatedAt());
        return m;
    }
}

