package com.zingzing.backend.controller;

import com.zingzing.backend.config.security.AuthenticatedUser;
import com.zingzing.backend.entity.DisputeCase;
import com.zingzing.backend.service.DisputeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/disputes")
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    public record OpenDisputeRequest(
            UUID orderId,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 2000) String description
    ) {}

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> openDispute(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody OpenDisputeRequest request
    ) {
        DisputeCase dispute = disputeService.openDispute(
                authUser.userId(), authUser.role(),
                request.orderId(), request.title(), request.description()
        );
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("data", toMap(dispute));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getMyDisputes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        var disputes = disputeService.getMyDisputes(authUser.userId(), authUser.role(), page, limit);
        List<Map<String, Object>> items = disputes.getContent().stream()
                .map(this::toMap)
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("disputes", items);
        result.put("total", disputes.getTotalElements());
        result.put("page", page);
        result.put("limit", limit);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{disputeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getDispute(
            @PathVariable UUID disputeId,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        DisputeCase dispute = disputeService.getDispute(disputeId, authUser.userId(), authUser.role());
        return ResponseEntity.ok(toMap(dispute));
    }

    private Map<String, Object> toMap(DisputeCase d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("orderId", d.getOrder().getId());
        m.put("orderNumber", d.getOrder().getOrderNumber());
        m.put("title", d.getTitle());
        m.put("description", d.getDescription());
        m.put("status", d.getStatus().name().toLowerCase());
        m.put("priority", d.getPriority());
        m.put("resolution", d.getResolution().name().toLowerCase());
        m.put("resolutionNotes", d.getResolutionNotes());
        m.put("resolvedAt", d.getResolvedAt());
        m.put("createdAt", d.getCreatedAt());
        m.put("updatedAt", d.getUpdatedAt());
        m.put("creatorId", d.getOrder().getCreator().getId());
        m.put("creatorName", d.getOrder().getCreator().getName());
        m.put("brandId", d.getOrder().getBrand().getId());
        m.put("brandName", d.getOrder().getBrand().getDisplayName());
        m.put("packageTitle", d.getOrder().getServicePackage().getTitle());
        return m;
    }
}
