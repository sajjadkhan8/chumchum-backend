package com.zingzing.backend.service;

import com.zingzing.backend.entity.*;
import com.zingzing.backend.entity.enums.*;
import com.zingzing.backend.entity.*;
import com.zingzing.backend.entity.enums.BrandPaymentAccessRole;
import com.zingzing.backend.entity.enums.BrandPaymentMethodStatus;
import com.zingzing.backend.entity.enums.BrandPaymentMethodType;
import com.zingzing.backend.entity.enums.UserRole;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.repository.*;
import com.zingzing.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BrandPaymentsService {

    private final BrandRepository brandRepository;
    private final BrandWalletRepository brandWalletRepository;
    private final BrandPaymentMethodRepository brandPaymentMethodRepository;
    private final BrandInvoiceRepository brandInvoiceRepository;
    private final BrandDisbursementRepository brandDisbursementRepository;
    private final BrandPayoutControlRepository brandPayoutControlRepository;
    private final BrandPaymentAccessRepository brandPaymentAccessRepository;
    private final PaymentAuditService paymentAuditService;

    public BrandPaymentsService(
            BrandRepository brandRepository,
            BrandWalletRepository brandWalletRepository,
            BrandPaymentMethodRepository brandPaymentMethodRepository,
            BrandInvoiceRepository brandInvoiceRepository,
            BrandDisbursementRepository brandDisbursementRepository,
            BrandPayoutControlRepository brandPayoutControlRepository,
            BrandPaymentAccessRepository brandPaymentAccessRepository,
            PaymentAuditService paymentAuditService
    ) {
        this.brandRepository = brandRepository;
        this.brandWalletRepository = brandWalletRepository;
        this.brandPaymentMethodRepository = brandPaymentMethodRepository;
        this.brandInvoiceRepository = brandInvoiceRepository;
        this.brandDisbursementRepository = brandDisbursementRepository;
        this.brandPayoutControlRepository = brandPayoutControlRepository;
        this.brandPaymentAccessRepository = brandPaymentAccessRepository;
        this.paymentAuditService = paymentAuditService;
    }

    public record BrandScope(UUID brandId, BrandPaymentAccessRole role) {}

    public record BrandPaymentSummaryResponse(
            int walletBalance,
            int monthlySpend,
            int pendingEscrow,
            int processingPayouts,
            Instant nextInvoiceDate
    ) {}

    public record BrandPaymentMethodResponse(
            UUID id,
            String type,
            String label,
            String accountMask,
            String holderName,
            boolean isDefault,
            String status,
            Instant createdAt
    ) {}

    public record BrandInvoiceResponse(
            UUID id,
            String periodLabel,
            int amount,
            String status,
            Instant issuedAt,
            Instant dueAt
    ) {}

    public record BrandInvoiceDetailResponse(
            UUID id,
            String periodLabel,
            int amount,
            String status,
            Instant issuedAt,
            Instant dueAt,
            List<Map<String, Object>> lineItems
    ) {}

    public record BrandDisbursementResponse(
            UUID id,
            String creatorName,
            String campaignName,
            int amount,
            String status,
            Instant releaseDate
    ) {}

    public record BrandPayoutControlsResponse(
            boolean requireTwoApprovals,
            int autoReleaseAfterDays,
            int lowBalanceAlertThreshold
    ) {}

    public record CreateBrandPaymentMethodRequest(
            BrandPaymentMethodType type,
            String label,
            String accountMask,
            String holderName,
            Boolean isDefault
    ) {}

    public record UpdateBrandPaymentMethodRequest(Boolean isDefault) {}

    public record UpdateControlsRequest(
            Boolean requireTwoApprovals,
            Integer autoReleaseAfterDays,
            Integer lowBalanceAlertThreshold
    ) {}

    public BrandScope resolveBrandScope(UUID actorId, UserRole actorRole, UUID requestedBrandId) {
        if (actorRole == null) throw new ApiException(HttpStatus.FORBIDDEN, "Authentication required");

        if (actorRole.isAdmin()) {
            if (requestedBrandId == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "brandId is required for admin context");
            }
            Brand brand = findBrand(requestedBrandId);
            return new BrandScope(brand.getId(), BrandPaymentAccessRole.ADMIN);
        }

        if (!actorRole.isBrand()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only brand users can access brand payments");
        }

        if (brandRepository.findById(actorId).isPresent()) {
            Brand ownBrand = findBrand(actorId);
            return new BrandScope(ownBrand.getId(), BrandPaymentAccessRole.OWNER);
        }

        List<BrandPaymentAccess> accessRows = brandPaymentAccessRepository.findByUserId(actorId);
        if (accessRows.isEmpty()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No brand payment access configured for this account");
        }

        BrandPaymentAccess access = requestedBrandId == null
                ? (accessRows.size() == 1 ? accessRows.get(0) : null)
                : accessRows.stream().filter(item -> item.getBrand().getId().equals(requestedBrandId)).findFirst().orElse(null);

        if (access == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Multiple brand access rows found; specify brandId");
        }

        return new BrandScope(access.getBrand().getId(), access.getRole());
    }

    @Transactional
    public BrandPaymentSummaryResponse getSummary(BrandScope scope) {
        Brand brand = managedBrand(scope);
        BrandWallet wallet = ensureWallet(brand);

        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        List<BrandDisbursement> disbursements = brandDisbursementRepository.findByBrandIdOrderByReleaseDateDesc(brand.getId());

        int monthlySpend = disbursements.stream()
                .filter(d -> d.getStatus() == BrandDisbursementStatus.COMPLETED
                        && d.getReleaseDate() != null && d.getReleaseDate().isAfter(thirtyDaysAgo))
                .mapToInt(BrandDisbursement::getAmount)
                .sum();

        int processingPayouts = disbursements.stream()
                .filter(d -> d.getStatus() == BrandDisbursementStatus.PROCESSING
                        || d.getStatus() == BrandDisbursementStatus.SCHEDULED)
                .mapToInt(BrandDisbursement::getAmount)
                .sum();

        return new BrandPaymentSummaryResponse(
                wallet.getWalletBalance(),
                monthlySpend,
                wallet.getPendingEscrow(),
                processingPayouts,
                wallet.getNextInvoiceDate()
        );
    }

    @Transactional
    public List<BrandPaymentMethodResponse> getMethods(BrandScope scope) {
        requireView(scope.role());
        return brandPaymentMethodRepository.findByBrandIdOrderByCreatedAtDesc(scope.brandId())
                .stream().map(this::toMethodResponse).toList();
    }

    @Transactional
    public BrandPaymentMethodResponse createMethod(UUID actorId, BrandScope scope, CreateBrandPaymentMethodRequest request) {
        requireManageFunds(scope.role(), "create payment methods");
        Brand brand = managedBrand(scope);

        if (request.type() == null) throw new ApiException(HttpStatus.BAD_REQUEST, "type is required");
        String label = normalize(request.label());
        String accountMask = normalize(request.accountMask());
        String holderName = normalize(request.holderName());
        if (label.isBlank() || accountMask.isBlank() || holderName.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "label, accountMask, and holderName are required");
        }

        if (Boolean.TRUE.equals(request.isDefault())) {
            clearDefaultMethod(scope.brandId());
        }

        BrandPaymentMethod method = brandPaymentMethodRepository.save(BrandPaymentMethod.builder()
                .brand(brand)
                .type(request.type())
                .label(label)
                .accountMask(accountMask)
                .holderName(holderName)
                .isDefault(Boolean.TRUE.equals(request.isDefault()) || noDefault(scope.brandId()))
                .status(BrandPaymentMethodStatus.ACTIVE)
                .build());

        paymentAuditService.log(actorId, brand, "BRAND_PAYMENT_METHOD_CREATED", "brand_payment_method",
                method.getId().toString(), "type=" + method.getType().name().toLowerCase());

        return toMethodResponse(method);
    }

    @Transactional
    public BrandPaymentMethodResponse updateMethod(UUID actorId, BrandScope scope, UUID methodId, UpdateBrandPaymentMethodRequest request) {
        requireManageFunds(scope.role(), "update payment methods");
        Brand brand = managedBrand(scope);

        BrandPaymentMethod method = findBrandMethod(scope.brandId(), methodId);
        if (Boolean.TRUE.equals(request.isDefault())) {
            clearDefaultMethod(scope.brandId());
            method.setDefault(true);
        }
        BrandPaymentMethod saved = brandPaymentMethodRepository.save(method);

        paymentAuditService.log(actorId, brand, "BRAND_PAYMENT_METHOD_UPDATED", "brand_payment_method",
                methodId.toString(), "isDefault=" + saved.isDefault());

        return toMethodResponse(saved);
    }

    @Transactional
    public void deleteMethod(UUID actorId, BrandScope scope, UUID methodId) {
        requireManageFunds(scope.role(), "delete payment methods");
        Brand brand = managedBrand(scope);

        BrandPaymentMethod method = findBrandMethod(scope.brandId(), methodId);
        boolean wasDefault = method.isDefault();
        brandPaymentMethodRepository.delete(method);

        if (wasDefault) {
            brandPaymentMethodRepository.findByBrandIdOrderByCreatedAtDesc(scope.brandId()).stream()
                    .findFirst().ifPresent(first -> {
                        first.setDefault(true);
                        brandPaymentMethodRepository.save(first);
                    });
        }

        paymentAuditService.log(actorId, brand, "BRAND_PAYMENT_METHOD_DELETED", "brand_payment_method",
                methodId.toString(), "deleted=true");
    }

    @Transactional
    public List<BrandInvoiceResponse> getInvoices(BrandScope scope) {
        requireView(scope.role());
        return brandInvoiceRepository.findByBrandIdOrderByIssuedAtDesc(scope.brandId())
                .stream().map(this::toInvoiceResponse).toList();
    }

    @Transactional
    public BrandInvoiceDetailResponse getInvoiceDetail(BrandScope scope, UUID invoiceId) {
        requireView(scope.role());
        BrandInvoice invoice = brandInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invoice not found"));
        if (!invoice.getBrand().getId().equals(scope.brandId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Invoice does not belong to this brand");
        }
        int serviceFee = Math.round(invoice.getAmount() * 0.1f);
        int campaignSpend = Math.max(0, invoice.getAmount() - serviceFee);
        return new BrandInvoiceDetailResponse(
                invoice.getId(),
                invoice.getPeriodLabel(),
                invoice.getAmount(),
                invoice.getStatus().name().toLowerCase(),
                invoice.getIssuedAt(),
                invoice.getDueAt(),
                List.of(
                        Map.of("description", "Campaign spend", "amount", campaignSpend),
                        Map.of("description", "Platform service fee", "amount", serviceFee)
                )
        );
    }

    @Transactional
    public List<BrandDisbursementResponse> getDisbursements(BrandScope scope) {
        requireView(scope.role());
        return brandDisbursementRepository.findByBrandIdOrderByReleaseDateDesc(scope.brandId())
                .stream().map(this::toDisbursementResponse).toList();
    }

    @Transactional
    public BrandPayoutControlsResponse getControls(BrandScope scope) {
        requireView(scope.role());
        Brand controlBrand = managedBrand(scope);
        BrandPayoutControl control = ensureControl(controlBrand);
        return toControlsResponse(control);
    }

    @Transactional
    public BrandPayoutControlsResponse updateControls(UUID actorId, BrandScope scope, UpdateControlsRequest request) {
        requireManageFunds(scope.role(), "update payout controls");
        Brand brand = managedBrand(scope);

        BrandPayoutControl control = ensureControl(brand);
        if (request.requireTwoApprovals() != null) {
            control.setRequireTwoApprovals(request.requireTwoApprovals());
        }
        if (request.autoReleaseAfterDays() != null) {
            control.setAutoReleaseAfterDays(Math.max(1, request.autoReleaseAfterDays()));
        }
        if (request.lowBalanceAlertThreshold() != null) {
            control.setLowBalanceAlertThreshold(Math.max(50000, request.lowBalanceAlertThreshold()));
        }

        BrandPayoutControl saved = brandPayoutControlRepository.save(control);
        paymentAuditService.log(actorId, brand, "BRAND_PAYOUT_CONTROLS_UPDATED", "brand_payout_controls",
                scope.brandId().toString(), "requireTwoApprovals=" + saved.isRequireTwoApprovals());

        return toControlsResponse(saved);
    }

    private Brand findBrand(UUID brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Brand profile not found"));
    }

    private Brand managedBrand(BrandScope scope) {
        return findBrand(scope.brandId());
    }

    private BrandWallet ensureWallet(Brand brand) {
        return brandWalletRepository.findById(brand.getId())
                .orElseGet(() -> brandWalletRepository.save(BrandWallet.builder().brand(brand).build()));
    }

    private BrandPayoutControl ensureControl(Brand brand) {
        return brandPayoutControlRepository.findById(brand.getId())
                .orElseGet(() -> brandPayoutControlRepository.save(BrandPayoutControl.builder().brand(brand).build()));
    }

    private boolean noDefault(UUID brandId) {
        return brandPaymentMethodRepository.findByBrandIdAndIsDefaultTrue(brandId).isEmpty();
    }

    private void clearDefaultMethod(UUID brandId) {
        brandPaymentMethodRepository.findByBrandIdAndIsDefaultTrue(brandId)
                .ifPresent(method -> {
                    method.setDefault(false);
                    brandPaymentMethodRepository.save(method);
                });
    }

    private BrandPaymentMethod findBrandMethod(UUID brandId, UUID methodId) {
        BrandPaymentMethod method = brandPaymentMethodRepository.findById(methodId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Brand payment method not found"));
        if (!method.getBrand().getId().equals(brandId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Payment method does not belong to this brand");
        }
        return method;
    }

    private BrandPaymentMethodResponse toMethodResponse(BrandPaymentMethod method) {
        return new BrandPaymentMethodResponse(
                method.getId(),
                method.getType().name().toLowerCase(),
                method.getLabel(),
                method.getAccountMask(),
                method.getHolderName(),
                method.isDefault(),
                method.getStatus().name().toLowerCase(),
                method.getCreatedAt()
        );
    }

    private BrandInvoiceResponse toInvoiceResponse(BrandInvoice invoice) {
        return new BrandInvoiceResponse(
                invoice.getId(),
                invoice.getPeriodLabel(),
                invoice.getAmount(),
                invoice.getStatus().name().toLowerCase(),
                invoice.getIssuedAt(),
                invoice.getDueAt()
        );
    }

    private BrandDisbursementResponse toDisbursementResponse(BrandDisbursement disbursement) {
        String creatorName = disbursement.getCreator() == null ? "Creator" : disbursement.getCreator().getName();
        return new BrandDisbursementResponse(
                disbursement.getId(),
                creatorName,
                disbursement.getCampaignName(),
                disbursement.getAmount(),
                disbursement.getStatus().name().toLowerCase(),
                disbursement.getReleaseDate()
        );
    }

    private BrandPayoutControlsResponse toControlsResponse(BrandPayoutControl control) {
        return new BrandPayoutControlsResponse(
                control.isRequireTwoApprovals(),
                control.getAutoReleaseAfterDays(),
                control.getLowBalanceAlertThreshold()
        );
    }

    private void requireView(BrandPaymentAccessRole role) {
        if (role == null || !role.canView()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Payment access required");
        }
    }

    private void requireManageFunds(BrandPaymentAccessRole role, String action) {
        if (role == null || !role.canManageFunds()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only owner/admin/finance can " + action);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
