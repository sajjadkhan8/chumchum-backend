package com.zingzing.backend.controller;

import com.zingzing.backend.config.security.AuthenticatedUser;
import com.zingzing.backend.entity.enums.BrandPaymentMethodType;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.service.BrandPaymentsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/brands/me/payments")
public class BrandPaymentsController {

    private final BrandPaymentsService brandPaymentsService;

    public BrandPaymentsController(BrandPaymentsService brandPaymentsService) {
        this.brandPaymentsService = brandPaymentsService;
    }

    public record CreateMethodRequest(
            @NotBlank String type,
            @NotBlank @Size(max = 100) String label,
            @NotBlank @Size(max = 120) String accountMask,
            @NotBlank @Size(max = 120) String holderName,
            Boolean isDefault
    ) {}

    public record UpdateMethodRequest(Boolean isDefault) {}

    public record TopUpRequest(@NotNull @Min(1000) Integer amount) {}

    public record UpdateControlsRequest(
            Boolean requireTwoApprovals,
            @Min(1) Integer autoReleaseAfterDays,
            @Min(50000) Integer lowBalanceAlertThreshold
    ) {}

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary(
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        var scope = resolveScope(authUser);
        return ok(brandPaymentsService.getSummary(scope));
    }

    @GetMapping("/methods")
    public ResponseEntity<Map<String, Object>> methods(
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        var scope = resolveScope(authUser);
        return ok(brandPaymentsService.getMethods(scope));
    }

    @PostMapping("/methods")
    public ResponseEntity<Map<String, Object>> createMethod(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody CreateMethodRequest request
    ) {
        var scope = resolveScope(authUser);
        BrandPaymentMethodType methodType;
        try {
            methodType = BrandPaymentMethodType.valueOf(request.type().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid payment method type: " + request.type());
        }

        var created = brandPaymentsService.createMethod(
                authUser.userId(),
                scope,
                new BrandPaymentsService.CreateBrandPaymentMethodRequest(
                        methodType,
                        request.label(),
                        request.accountMask(),
                        request.holderName(),
                        request.isDefault()
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "data", created));
    }

    @PatchMapping("/methods/{id}")
    public ResponseEntity<Map<String, Object>> updateMethod(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID id,
            @RequestBody UpdateMethodRequest request
    ) {
        var scope = resolveScope(authUser);
        return ok(brandPaymentsService.updateMethod(authUser.userId(), scope, id,
                new BrandPaymentsService.UpdateBrandPaymentMethodRequest(request.isDefault())));
    }

    @DeleteMapping("/methods/{id}")
    public ResponseEntity<Map<String, Object>> deleteMethod(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID id
    ) {
        var scope = resolveScope(authUser);
        brandPaymentsService.deleteMethod(authUser.userId(), scope, id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Payment method deleted"));
    }

    @GetMapping("/invoices")
    public ResponseEntity<Map<String, Object>> invoices(
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        var scope = resolveScope(authUser);
        return ok(brandPaymentsService.getInvoices(scope));
    }

    @GetMapping("/disbursements")
    public ResponseEntity<Map<String, Object>> disbursements(
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        var scope = resolveScope(authUser);
        return ok(brandPaymentsService.getDisbursements(scope));
    }

    @GetMapping("/controls")
    public ResponseEntity<Map<String, Object>> controls(
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        var scope = resolveScope(authUser);
        return ok(brandPaymentsService.getControls(scope));
    }

    @PatchMapping("/controls")
    public ResponseEntity<Map<String, Object>> updateControls(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody UpdateControlsRequest request
    ) {
        var scope = resolveScope(authUser);
        return ok(brandPaymentsService.updateControls(authUser.userId(), scope,
                new BrandPaymentsService.UpdateControlsRequest(
                        request.requireTwoApprovals(),
                        request.autoReleaseAfterDays(),
                        request.lowBalanceAlertThreshold()
                )));
    }

    @PostMapping("/top-up")
    public ResponseEntity<Map<String, Object>> topUp(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody TopUpRequest request
    ) {
        var scope = resolveScope(authUser);
        return ok(brandPaymentsService.topUp(authUser.userId(), scope, new BrandPaymentsService.TopUpRequest(request.amount())));
    }

    private BrandPaymentsService.BrandScope resolveScope(AuthenticatedUser authUser) {
        return brandPaymentsService.resolveBrandScope(authUser.userId(), authUser.role(), null);
    }

    private ResponseEntity<Map<String, Object>> ok(Object data) {
        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }
}

