package com.chamcham.backend.controller;

import com.chamcham.backend.config.security.AuthenticatedUser;
import com.chamcham.backend.entity.PayoutMethod;
import com.chamcham.backend.entity.enums.PayoutMethodType;
import com.chamcham.backend.service.PayoutMethodService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payout-methods")
public class PayoutMethodController {

    private final PayoutMethodService payoutMethodService;

    public PayoutMethodController(PayoutMethodService payoutMethodService) {
        this.payoutMethodService = payoutMethodService;
    }

    public record CreatePayoutMethodRequest(
            @NotNull PayoutMethodType type,
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 300) String accountDetails,
            boolean isDefault
    ) {}

    public record UpdatePayoutMethodRequest(
            @Size(max = 100) String name,
            @Size(max = 300) String accountDetails,
            Boolean isDefault
    ) {}

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(@AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.ok(Map.of("success", true, "data",
                payoutMethodService.list(authUser.userId(), authUser.role())
                        .stream().map(this::toMap).toList()));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody CreatePayoutMethodRequest req
    ) {
        PayoutMethod pm = payoutMethodService.create(
                authUser.userId(), authUser.role(),
                req.type(), req.name(), req.accountDetails(), req.isDefault());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("success", true, "data", toMap(pm)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody UpdatePayoutMethodRequest req
    ) {
        PayoutMethod pm = payoutMethodService.update(id, authUser.userId(), req.name(), req.accountDetails(), req.isDefault());
        return ResponseEntity.ok(Map.of("success", true, "data", toMap(pm)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        payoutMethodService.delete(id, authUser.userId());
        return ResponseEntity.ok(Map.of("success", true, "message", "Payout method deleted"));
    }

    private Map<String, Object> toMap(PayoutMethod pm) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", pm.getId());
        m.put("type", pm.getType().name().toLowerCase());
        m.put("name", pm.getName());
        m.put("accountDetails", maskAccount(pm.getAccountDetails()));
        m.put("isDefault", pm.isDefault());
        m.put("createdAt", pm.getCreatedAt());
        return m;
    }

    private String maskAccount(String raw) {
        if (raw == null || raw.length() < 4) return "****";
        return "*".repeat(raw.length() - 4) + raw.substring(raw.length() - 4);
    }
}

