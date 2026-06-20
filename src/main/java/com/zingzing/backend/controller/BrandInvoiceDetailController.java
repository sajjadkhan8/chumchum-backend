package com.zingzing.backend.controller;

import com.zingzing.backend.config.security.AuthenticatedUser;
import com.zingzing.backend.service.BrandPaymentsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/brand/payments/invoices")
public class BrandInvoiceDetailController {

    private final BrandPaymentsService brandPaymentsService;

    public BrandInvoiceDetailController(BrandPaymentsService brandPaymentsService) {
        this.brandPaymentsService = brandPaymentsService;
    }

    @GetMapping("/{invoiceId}")
    public ResponseEntity<Map<String, Object>> invoiceDetail(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID invoiceId
    ) {
        var scope = brandPaymentsService.resolveBrandScope(authUser.userId(), authUser.role(), null);
        return ResponseEntity.ok(Map.of("success", true, "data", brandPaymentsService.getInvoiceDetail(scope, invoiceId)));
    }
}
