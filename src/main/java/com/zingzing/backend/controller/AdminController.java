package com.zingzing.backend.controller;

import com.zingzing.backend.config.security.AuthenticatedUser;
import com.zingzing.backend.dto.order.OrderResponse;
import com.zingzing.backend.entity.AmbassadorApplication;
import com.zingzing.backend.entity.AmbassadorScore;
import com.zingzing.backend.entity.ApiLog;
import com.zingzing.backend.entity.Brand;
import com.zingzing.backend.entity.BrandVerificationDocument;
import com.zingzing.backend.entity.BrandVerificationEvent;
import com.zingzing.backend.entity.Creator;
import com.zingzing.backend.entity.CreatorVerificationDocument;
import com.zingzing.backend.entity.CreatorVerificationEvent;
import com.zingzing.backend.entity.Order;
import com.zingzing.backend.entity.User;
import com.zingzing.backend.entity.WithdrawalRequest;
import com.zingzing.backend.entity.enums.BrandPlanTier;
import com.zingzing.backend.entity.enums.OrderStatus;
import com.zingzing.backend.entity.enums.CreatorBadgeLevel;
import com.zingzing.backend.entity.enums.UserRole;
import com.zingzing.backend.entity.enums.AmbassadorAppStatus;
import com.zingzing.backend.entity.enums.WithdrawalStatus;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.mapper.BrandMapper;
import com.zingzing.backend.mapper.CreatorMapper;
import com.zingzing.backend.mapper.OrderMapper;
import com.zingzing.backend.mapper.UserMapper;
import com.zingzing.backend.repository.BrandRepository;
import com.zingzing.backend.repository.BrandVerificationDocumentRepository;
import com.zingzing.backend.repository.BrandVerificationEventRepository;
import com.zingzing.backend.repository.CreatorRepository;
import com.zingzing.backend.repository.CreatorVerificationDocumentRepository;
import com.zingzing.backend.repository.CreatorVerificationEventRepository;
import com.zingzing.backend.repository.OrderRepository;
import com.zingzing.backend.repository.AmbassadorScoreRepository;
import com.zingzing.backend.repository.ApiLogRepository;
import com.zingzing.backend.repository.ReviewRepository;
import com.zingzing.backend.repository.UserRepository;
import com.zingzing.backend.repository.WithdrawalRequestRepository;
import com.zingzing.backend.service.AdminOperationsService;
import com.zingzing.backend.service.AdminStepUpService;
import com.zingzing.backend.service.AmbassadorService;
import com.zingzing.backend.service.OrderService;
import com.zingzing.backend.service.WithdrawalService;
import com.zingzing.backend.util.BrandVerificationStatuses;
import com.zingzing.backend.util.CreatorVerificationStatuses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final CreatorRepository creatorRepository;
    private final BrandRepository brandRepository;
    private final OrderRepository orderRepository;
    private final UserMapper userMapper;
    private final CreatorMapper creatorMapper;
    private final BrandMapper brandMapper;
    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final AmbassadorService ambassadorService;
    private final AdminOperationsService adminOperationsService;
    private final WithdrawalService withdrawalService;
    private final AdminStepUpService adminStepUpService;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final ReviewRepository reviewRepository;
    private final AmbassadorScoreRepository ambassadorScoreRepository;
    private final ApiLogRepository apiLogRepository;
    private final BrandVerificationDocumentRepository brandVerificationDocumentRepository;
    private final BrandVerificationEventRepository brandVerificationEventRepository;
    private final CreatorVerificationDocumentRepository creatorVerificationDocumentRepository;
    private final CreatorVerificationEventRepository creatorVerificationEventRepository;

    public AdminController(UserRepository userRepository,
                           CreatorRepository creatorRepository,
                           BrandRepository brandRepository,
                           OrderRepository orderRepository,
                           UserMapper userMapper,
                           CreatorMapper creatorMapper,
                           BrandMapper brandMapper,
                           OrderMapper orderMapper,
                           OrderService orderService,
                           AmbassadorService ambassadorService,
                           AdminOperationsService adminOperationsService,
                           WithdrawalService withdrawalService,
                           AdminStepUpService adminStepUpService,
                           WithdrawalRequestRepository withdrawalRequestRepository,
                           ReviewRepository reviewRepository,
                           AmbassadorScoreRepository ambassadorScoreRepository,
                           ApiLogRepository apiLogRepository,
                           BrandVerificationDocumentRepository brandVerificationDocumentRepository,
                           BrandVerificationEventRepository brandVerificationEventRepository,
                           CreatorVerificationDocumentRepository creatorVerificationDocumentRepository,
                           CreatorVerificationEventRepository creatorVerificationEventRepository) {
        this.userRepository = userRepository;
        this.creatorRepository = creatorRepository;
        this.brandRepository = brandRepository;
        this.orderRepository = orderRepository;
        this.userMapper = userMapper;
        this.creatorMapper = creatorMapper;
        this.brandMapper = brandMapper;
        this.orderMapper = orderMapper;
        this.orderService = orderService;
        this.ambassadorService = ambassadorService;
        this.adminOperationsService = adminOperationsService;
        this.withdrawalService = withdrawalService;
        this.adminStepUpService = adminStepUpService;
        this.withdrawalRequestRepository = withdrawalRequestRepository;
        this.reviewRepository = reviewRepository;
        this.ambassadorScoreRepository = ambassadorScoreRepository;
        this.apiLogRepository = apiLogRepository;
        this.brandVerificationDocumentRepository = brandVerificationDocumentRepository;
        this.brandVerificationEventRepository = brandVerificationEventRepository;
        this.creatorVerificationDocumentRepository = creatorVerificationDocumentRepository;
        this.creatorVerificationEventRepository = creatorVerificationEventRepository;
    }

    public record UserStatusRequest(@NotNull Boolean active) {}

    public record UserModerateRequest(
            @NotNull @NotBlank @Size(max = 20) String action,  // "suspend" | "ban" | "unban"
            @Size(max = 500) String reason,
            Integer suspendDays
    ) {}

    public record BulkModerateRequest(
            @NotNull List<UUID> userIds,
            @NotNull @NotBlank @Size(max = 20) String action,
            @Size(max = 500) String reason,
            Integer suspendDays
    ) {}

    public record WithdrawalStatusRequest(@NotNull @NotBlank @Size(max = 20) String status) {}

    public record CreatorVerificationRequest(@NotNull Boolean verified) {}

    public record CreatorBadgeRequest(@NotNull String badgeLevel) {}

    public record BrandVerificationRequest(
            @Size(max = 50) String status,
            @Size(max = 255) String contactEmail,
            @Size(max = 50) String phoneNumber
    ) {}

    public record BrandVerificationDocumentReviewRequest(
            @NotNull @NotBlank @Size(max = 30) String status,
            @Size(max = 500) String reason
    ) {}

    public record BrandVerificationDecisionRequest(
            @NotNull @NotBlank @Size(max = 30) String decision,
            @Size(max = 500) String reason,
            @Size(max = 255) String contactEmail,
            @Size(max = 50) String phoneNumber
    ) {}

    public record OrderStatusRequest(@NotNull String status) {}

    public record AmbassadorReviewRequest(
            @NotNull @Size(max = 30) String status,
            @Size(max = 2000) String notes
    ) {}

    @GetMapping("/sla-metrics")
    public ResponseEntity<Map<String, Object>> slaMetrics(@AuthenticationPrincipal AuthenticatedUser authUser) {
        requireAdmin(authUser);

        OffsetDateTime now = OffsetDateTime.now();

        // On-time delivery rate for completed orders with a deadline
        List<Object[]> onTimeRows = orderRepository.countOnTimeVsTotal();
        Object[] onTimeRow = onTimeRows.isEmpty() ? null : onTimeRows.get(0);
        long onTimeCount = onTimeRow != null && onTimeRow[0] != null ? ((Number) onTimeRow[0]).longValue() : 0L;
        long totalWithDeadline = onTimeRow != null && onTimeRow[1] != null ? ((Number) onTimeRow[1]).longValue() : 0L;
        double ordersCompletedOnTimePct = totalWithDeadline > 0 ? Math.round((onTimeCount * 1000.0 / totalWithDeadline)) / 10.0 : 0.0;

        // Avg resolution time in days (updatedAt - createdAt for COMPLETED orders)
        Double avgResolutionHours = orderRepository.avgResolutionHours();
        double avgDisputeResolutionDays = avgResolutionHours != null ? Math.round(avgResolutionHours / 24.0 * 10.0) / 10.0 : 0.0;

        // Approximate: (COMPLETED withdrawals / total) * 100 as a proxy for within-24h processing rate
        long completedWithdrawals = withdrawalRequestRepository.countByStatus(WithdrawalStatus.COMPLETED);
        long totalWithdrawals = withdrawalRequestRepository.count();
        double withdrawalsProcessedWithin24hPct = totalWithdrawals > 0 ? Math.round((completedWithdrawals * 1000.0 / totalWithdrawals)) / 10.0 : 0.0;

        // Pending verifications
        long pendingCreatorVerifications = creatorRepository.countUnverifiedActive();
        long pendingBrandVerifications = brandRepository.countPendingVerifications();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("avgDisputeResolutionDays", avgDisputeResolutionDays);
        data.put("withdrawalsProcessedWithin24hPct", withdrawalsProcessedWithin24hPct);
        data.put("ordersCompletedOnTimePct", ordersCompletedOnTimePct);
        data.put("pendingCreatorVerifications", pendingCreatorVerifications);
        data.put("pendingBrandVerifications", pendingBrandVerifications);
        return ok(data);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard(@AuthenticationPrincipal AuthenticatedUser authUser) {
        requireAdmin(authUser);

        // Use SQL aggregates — never load all orders into JVM heap
        long totalOrders   = orderRepository.count();
        long completedRevenue = orderRepository.sumAmountByStatus(OrderStatus.COMPLETED);
        long gmv           = orderRepository.sumTotalGmv();

        Map<String, Object> statusCounts = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            statusCounts.put(status.name().toLowerCase(), orderRepository.countByStatus(status));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("users", Map.of(
                "total", userRepository.count(),
                "creators", userRepository.countByRole(UserRole.CREATOR),
                "brands", userRepository.countByRole(UserRole.BRAND),
                "admins", userRepository.countByRole(UserRole.PLATFORM_ADMIN),
                "active", userRepository.countByActiveTrue(),
                "inactive", userRepository.countByActiveFalse()
        ));
        data.put("orders", Map.of(
                "total", totalOrders,
                "byStatus", statusCounts
        ));
        data.put("revenue", Map.of(
                "gmv", gmv,
                "completedOrderAmount", completedRevenue
        ));
        List<Map<String, Object>> ordersByCategory = orderRepository.countCompletedOrdersByCategory()
                .stream()
                .map(row -> Map.<String, Object>of(
                        "category", row[0] == null ? "uncategorized" : row[0].toString(),
                        "count", row[1]))
                .toList();
        data.put("ordersByCategory", ordersByCategory);
        return ok(data);
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> users(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int limit
    ) {
        requireAdmin(authUser);
        UserRole roleFilter = parseRoleFilter(role);
        Page<User> result = userRepository.searchForAdmin(blankToNull(search), roleFilter, active, PageRequest.of(safePage(page), safeLimit(limit)));
        return ok(Map.of(
                "users", result.getContent().stream().map(userMapper::toResponse).toList(),
                "total", result.getTotalElements(),
                "page", result.getNumber(),
                "limit", result.getSize()
        ));
    }

    @PatchMapping("/users/{id}/status")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateUserStatus(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID id,
            @Valid @RequestBody UserStatusRequest request
    ) {
        requireAdmin(authUser);
        if (authUser.userId().equals(id) && !request.active()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Admins cannot disable their own account");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        user.setActive(request.active());
        User saved = userRepository.save(user);
        adminOperationsService.log(authUser.userId(), "USER_STATUS_CHANGED", "user", id.toString(), "active=" + request.active());
        return ok(userMapper.toResponse(saved));
    }

    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> orders(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        requireAdmin(authUser);
        int safePage = safePage(page);
        int safeLimit = safeLimit(limit);
        OrderStatus statusFilter = parseOrderStatusFilter(status);
        String searchFilter = blankToNull(search);
        Page<Order> orderPage = orderRepository.findForAdminPaged(statusFilter, searchFilter, PageRequest.of(safePage, safeLimit));
        List<OrderResponse> responses = orderPage.getContent().stream().map(orderMapper::toResponse).toList();
        return ok(Map.of(
                "orders", responses,
                "total", orderPage.getTotalElements(),
                "page", safePage,
                "limit", safeLimit
        ));
    }

    @PatchMapping("/orders/{id}/status")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateOrderStatus(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID id,
            @Valid @RequestBody OrderStatusRequest request
    ) {
        requireAdmin(authUser);
        OrderStatus status;
        try {
            status = OrderStatus.valueOf(request.status().trim().toUpperCase());
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid status: " + request.status());
        }
        var response = orderService.updateStatus(id, status, authUser.userId(), authUser.role());
        adminOperationsService.log(authUser.userId(), "ORDER_STATUS_CHANGED", "order", id.toString(), "status=" + status.name().toLowerCase());
        return ok(response);
    }

    @PatchMapping("/creators/{id}/verification")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateCreatorVerification(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID id,
            @Valid @RequestBody CreatorVerificationRequest request
    ) {
        requireAdmin(authUser);
        Creator creator = creatorRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator not found"));
        if (Boolean.TRUE.equals(request.verified()) && !hasAllRequiredApprovedCreatorDocuments(id)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Approve identity, social profile, and portfolio sample documents before verifying this creator");
        }
        creator.setVerified(request.verified());
        creator.setBadgeLevel(request.verified() ? CreatorBadgeLevel.VERIFIED : CreatorBadgeLevel.NONE);
        creator.setVerificationStatus(request.verified() ? CreatorVerificationStatuses.VERIFIED : CreatorVerificationStatuses.UNVERIFIED);
        Creator saved = creatorRepository.save(creator);
        logCreatorVerificationEvent(saved, null, authUser.userId(), "STATUS_UPDATED",
                "verified=" + request.verified() + ", badgeLevel=" + saved.getBadgeLevel().name().toLowerCase());
        adminOperationsService.log(authUser.userId(), "CREATOR_VERIFICATION_CHANGED", "creator", id.toString(),
                "verified=" + request.verified() + ", badgeLevel=" + saved.getBadgeLevel().name().toLowerCase());
        return ok(creatorMapper.toResponse(saved));
    }

    @GetMapping("/creators/{creatorId}/verification-evidence")
    public ResponseEntity<Map<String, Object>> getCreatorVerificationEvidence(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID creatorId
    ) {
        requireAdmin(authUser);
        Creator creator = findCreator(creatorId);
        List<Map<String, Object>> docs = creatorVerificationDocumentRepository.findByCreatorIdOrderByUploadedAtDesc(creatorId)
                .stream()
                .map(this::toCreatorVerificationDocumentMap)
                .toList();
        List<Map<String, Object>> events = creatorVerificationEventRepository.findByCreatorIdOrderByCreatedAtDesc(creatorId)
                .stream()
                .map(this::toCreatorVerificationEventMap)
                .toList();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("creator", creatorMapper.toResponse(creator));
        payload.put("documents", docs);
        payload.put("events", events);
        payload.put("requiredDocumentTypes", requiredCreatorDocumentTypes());
        payload.put("canApprove", hasAllRequiredApprovedCreatorDocuments(creatorId));
        return ok(payload);
    }

    @PatchMapping("/creators/{creatorId}/verification-documents/{documentId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> reviewCreatorVerificationDocument(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID creatorId,
            @PathVariable UUID documentId,
            @Valid @RequestBody BrandVerificationDocumentReviewRequest request
    ) {
        requireAdmin(authUser);
        Creator creator = findCreator(creatorId);
        CreatorVerificationDocument doc = creatorVerificationDocumentRepository.findByIdAndCreatorId(documentId, creatorId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Verification document not found"));
        String status = normalizeDocumentStatus(request.status());
        if ("REJECTED".equals(status) && (request.reason() == null || request.reason().isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A rejection reason is required");
        }
        User reviewer = userRepository.findById(authUser.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reviewer not found"));
        doc.setStatus(status);
        doc.setRejectionReason("REJECTED".equals(status) ? request.reason().trim() : null);
        doc.setReviewedBy(reviewer);
        doc.setReviewedAt(Instant.now());
        CreatorVerificationDocument saved = creatorVerificationDocumentRepository.save(doc);
        if ("REJECTED".equals(status)) {
            creator.setVerificationStatus(CreatorVerificationStatuses.REJECTED);
            creator.setVerified(false);
            creator.setBadgeLevel(CreatorBadgeLevel.NONE);
            creatorRepository.save(creator);
        } else if (CreatorVerificationStatuses.UNVERIFIED.equals(CreatorVerificationStatuses.normalizeForResponse(creator.getVerificationStatus()))
                || CreatorVerificationStatuses.PENDING.equals(CreatorVerificationStatuses.normalizeForResponse(creator.getVerificationStatus()))) {
            creator.setVerificationStatus(CreatorVerificationStatuses.UNDER_REVIEW);
            creatorRepository.save(creator);
        }
        logCreatorVerificationEvent(creator, saved, authUser.userId(), "DOCUMENT_" + status, request.reason());
        adminOperationsService.log(authUser.userId(), "CREATOR_VERIFICATION_DOCUMENT_" + status, "creator_verification_document",
                documentId.toString(), "creator=" + creatorId);
        return ok(toCreatorVerificationDocumentMap(saved));
    }

    @PostMapping("/creators/{creatorId}/verification-review")
    @Transactional
    public ResponseEntity<Map<String, Object>> decideCreatorVerification(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID creatorId,
            @Valid @RequestBody BrandVerificationDecisionRequest request
    ) {
        requireAdmin(authUser);
        Creator creator = findCreator(creatorId);
        String decision = CreatorVerificationStatuses.normalize(request.decision());
        if (CreatorVerificationStatuses.REJECTED.equals(decision) && (request.reason() == null || request.reason().isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A rejection reason is required");
        }
        if (CreatorVerificationStatuses.VERIFIED.equals(decision) && !hasAllRequiredApprovedCreatorDocuments(creatorId)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Approve identity, social profile, and portfolio sample documents before verifying this creator");
        }
        creator.setVerificationStatus(decision);
        creator.setVerified(CreatorVerificationStatuses.VERIFIED.equals(decision));
        creator.setBadgeLevel(CreatorVerificationStatuses.VERIFIED.equals(decision) ? CreatorBadgeLevel.VERIFIED : CreatorBadgeLevel.NONE);
        Creator saved = creatorRepository.save(creator);
        logCreatorVerificationEvent(creator, null, authUser.userId(), "FINAL_DECISION_" + decision.toUpperCase(), request.reason());
        adminOperationsService.log(authUser.userId(), "CREATOR_VERIFICATION_DECIDED", "creator", creatorId.toString(),
                "decision=" + decision + (request.reason() == null ? "" : ", reason=" + request.reason()));
        return ok(creatorMapper.toResponse(saved));
    }

    @PatchMapping("/creators/{id}/badge")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateCreatorBadge(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID id,
            @Valid @RequestBody CreatorBadgeRequest request
    ) {
        requireAdmin(authUser);
        Creator creator = creatorRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator not found"));
        CreatorBadgeLevel badgeLevel;
        try {
            badgeLevel = CreatorBadgeLevel.valueOf(request.badgeLevel().trim().toUpperCase());
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid creator badge level: " + request.badgeLevel());
        }
        if (!creator.isVerified() && badgeLevel != CreatorBadgeLevel.NONE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Verify the creator before assigning a badge");
        }
        if (creator.isVerified() && badgeLevel == CreatorBadgeLevel.NONE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Remove creator verification before clearing the badge");
        }
        creator.setBadgeLevel(badgeLevel);
        Creator saved = creatorRepository.save(creator);
        adminOperationsService.log(authUser.userId(), "CREATOR_BADGE_CHANGED", "creator", id.toString(),
                "badgeLevel=" + badgeLevel.name().toLowerCase());
        return ok(creatorMapper.toResponse(saved));
    }

    @GetMapping("/creators")
    public ResponseEntity<Map<String, Object>> creators(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        requireAdmin(authUser);
        Page<Creator> result = creatorRepository.searchForAdmin(
                blankToNull(search),
                verified,
                PageRequest.of(safePage(page), safeLimit(limit))
        );
        return ok(Map.of(
                "creators", result.getContent().stream().map(creatorMapper::toResponse).toList(),
                "total", result.getTotalElements(),
                "page", result.getNumber(),
                "limit", result.getSize()
        ));
    }

    @PatchMapping("/brands/{id}/verification")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateBrandVerification(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID id,
            @Valid @RequestBody BrandVerificationRequest request
    ) {
        requireAdmin(authUser);
        Brand brand = findBrand(id);
        if (request.status() != null) brand.setBusinessVerificationStatus(BrandVerificationStatuses.normalize(request.status()));
        if (request.contactEmail() != null) brand.setVerificationContactEmail(request.contactEmail());
        if (request.phoneNumber() != null) brand.setVerificationPhoneNumber(request.phoneNumber());
        Brand saved = brandRepository.save(brand);
        logBrandVerificationEvent(brand, null, authUser.userId(), "STATUS_UPDATED", "status=" + saved.getBusinessVerificationStatus());
        adminOperationsService.log(authUser.userId(), "BRAND_VERIFICATION_CHANGED", "brand", id.toString(), "status=" + saved.getBusinessVerificationStatus());
        return ok(brandMapper.toResponse(saved));
    }

    @GetMapping("/brands/{brandId}/verification-evidence")
    public ResponseEntity<Map<String, Object>> getBrandVerificationEvidence(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID brandId
    ) {
        requireAdmin(authUser);
        Brand brand = findBrand(brandId);
        List<Map<String, Object>> docs = brandVerificationDocumentRepository.findByBrandIdOrderByUploadedAtDesc(brandId)
                .stream()
                .map(this::toVerificationDocumentMap)
                .toList();
        List<Map<String, Object>> events = brandVerificationEventRepository.findByBrandIdOrderByCreatedAtDesc(brandId)
                .stream()
                .map(this::toVerificationEventMap)
                .toList();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("brand", brandMapper.toResponse(brand));
        payload.put("documents", docs);
        payload.put("events", events);
        payload.put("requiredDocumentTypes", List.of("tax_id", "business_registration", "bank_details"));
        payload.put("canApprove", hasAllRequiredApprovedDocuments(brandId));
        return ok(payload);
    }

    @PatchMapping("/brands/{brandId}/verification-documents/{documentId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> reviewBrandVerificationDocument(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID brandId,
            @PathVariable UUID documentId,
            @Valid @RequestBody BrandVerificationDocumentReviewRequest request
    ) {
        requireAdmin(authUser);
        Brand brand = findBrand(brandId);
        BrandVerificationDocument doc = brandVerificationDocumentRepository.findByIdAndBrandId(documentId, brandId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Verification document not found"));
        String status = normalizeDocumentStatus(request.status());
        if ("REJECTED".equals(status) && (request.reason() == null || request.reason().isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A rejection reason is required");
        }
        User reviewer = userRepository.findById(authUser.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reviewer not found"));
        doc.setStatus(status);
        doc.setRejectionReason("REJECTED".equals(status) ? request.reason().trim() : null);
        doc.setReviewedBy(reviewer);
        doc.setReviewedAt(Instant.now());
        BrandVerificationDocument saved = brandVerificationDocumentRepository.save(doc);
        if ("REJECTED".equals(status)) {
            brand.setBusinessVerificationStatus(BrandVerificationStatuses.REJECTED);
            brandRepository.save(brand);
        } else if (brand.getBusinessVerificationStatus() == null || brand.getBusinessVerificationStatus().isBlank()
                || BrandVerificationStatuses.PENDING.equals(BrandVerificationStatuses.normalizeForResponse(brand.getBusinessVerificationStatus()))) {
            brand.setBusinessVerificationStatus(BrandVerificationStatuses.UNDER_REVIEW);
            brandRepository.save(brand);
        }
        logBrandVerificationEvent(brand, saved, authUser.userId(), "DOCUMENT_" + status, request.reason());
        adminOperationsService.log(authUser.userId(), "BRAND_VERIFICATION_DOCUMENT_" + status, "brand_verification_document",
                documentId.toString(), "brand=" + brandId);
        return ok(toVerificationDocumentMap(saved));
    }

    @PostMapping("/brands/{brandId}/verification-review")
    @Transactional
    public ResponseEntity<Map<String, Object>> decideBrandVerification(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID brandId,
            @Valid @RequestBody BrandVerificationDecisionRequest request
    ) {
        requireAdmin(authUser);
        Brand brand = findBrand(brandId);
        String decision = BrandVerificationStatuses.normalize(request.decision());
        if ("rejected".equals(decision) && (request.reason() == null || request.reason().isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A rejection reason is required");
        }
        if ("verified".equals(decision) && !hasAllRequiredApprovedDocuments(brandId)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Approve tax, registration, and bank documents before verifying this brand");
        }
        if (request.contactEmail() != null) brand.setVerificationContactEmail(request.contactEmail());
        if (request.phoneNumber() != null) brand.setVerificationPhoneNumber(request.phoneNumber());
        brand.setBusinessVerificationStatus(decision);
        Brand saved = brandRepository.save(brand);
        logBrandVerificationEvent(brand, null, authUser.userId(), "FINAL_DECISION_" + decision.toUpperCase(), request.reason());
        adminOperationsService.log(authUser.userId(), "BRAND_VERIFICATION_DECIDED", "brand", brandId.toString(),
                "decision=" + decision + (request.reason() == null ? "" : ", reason=" + request.reason()));
        return ok(brandMapper.toResponse(saved));
    }

    @GetMapping("/brands")
    public ResponseEntity<Map<String, Object>> brands(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String verificationStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        requireAdmin(authUser);
        Page<Brand> result = brandRepository.searchForAdmin(
                blankToNull(search),
                verificationStatus == null || verificationStatus.isBlank() || verificationStatus.equalsIgnoreCase("all")
                        ? null
                        : BrandVerificationStatuses.normalize(verificationStatus),
                PageRequest.of(safePage(page), safeLimit(limit))
        );
        return ok(Map.of(
                "brands", result.getContent().stream().map(brandMapper::toResponse).toList(),
                "total", result.getTotalElements(),
                "page", result.getNumber(),
                "limit", result.getSize()
        ));
    }

    @PatchMapping("/brands/{brandId}/plan")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateBrandPlan(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID brandId,
            @RequestBody Map<String, String> body
    ) {
        requireAdmin(authUser);
        String tierStr = body.getOrDefault("plan_tier", "").trim().toUpperCase();
        BrandPlanTier tier;
        try {
            tier = BrandPlanTier.valueOf(tierStr);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid plan tier. Allowed: STARTER, GROWTH, ENTERPRISE");
        }
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Brand not found"));
        brand.setPlanTier(tier);
        brandRepository.save(brand);
        return ok(Map.of("success", true, "plan_tier", tier.name()));
    }

    @GetMapping("/ambassador/applications")
    public ResponseEntity<Map<String, Object>> ambassadorApplications(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int limit
    ) {
        requireAdmin(authUser);
        PageRequest pageable = PageRequest.of(safePage(page), safeLimit(limit));
        var applications = ambassadorService.listApplications(search, status, pageable);
        return ok(Map.of(
                "applications", applications.getContent().stream().map(this::toApplicationMap).toList(),
                "total", applications.getTotalElements(),
                "page", applications.getNumber(),
                "limit", applications.getSize()
        ));
    }

    @PatchMapping("/ambassador/applications/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> reviewAmbassadorApplication(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID id,
            @Valid @RequestBody AmbassadorReviewRequest request
    ) {
        requireAdmin(authUser);
        try {
            var reviewed = ambassadorService.reviewApplication(
                    id,
                    AmbassadorAppStatus.valueOf(request.status().trim().toUpperCase()),
                    authUser.userId(),
                    request.notes()
            );
            adminOperationsService.log(authUser.userId(), "AMBASSADOR_APPLICATION_REVIEWED", "ambassador_application", id.toString(), "status=" + reviewed.getStatus().name().toLowerCase());
            return ok(toApplicationMap(reviewed));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid status: " + request.status());
        }
    }

    @PatchMapping("/users/{id}/moderate")
    @Transactional
    public ResponseEntity<Map<String, Object>> moderateUser(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID id,
            @Valid @RequestBody UserModerateRequest request,
            @RequestHeader(value = "X-Step-Up-Token", required = false) String stepUpToken
    ) {
        requireAdmin(authUser);
        if (!authUser.role().canModerateUsers()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Support or Platform Admin role required for user moderation");
        }
        // Ban and suspend are destructive and irreversible — require step-up re-auth
        String action = request.action().trim().toLowerCase();
        if ("ban".equals(action) || "suspend".equals(action)) {
            adminStepUpService.requireStepUp(authUser, stepUpToken);
        }
        if (authUser.userId().equals(id)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Admins cannot moderate their own account");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() == UserRole.PLATFORM_ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cannot moderate other admin accounts");
        }

        switch (action) {
            case "ban" -> {
                user.setActive(false);
                user.setBanReason(request.reason());
                user.setSuspendedUntil(null);
                user.setDeletedAt(OffsetDateTime.now());
            }
            case "suspend" -> {
                int days = request.suspendDays() != null && request.suspendDays() > 0 ? request.suspendDays() : 30;
                user.setActive(false);
                user.setBanReason(request.reason());
                user.setSuspendedUntil(OffsetDateTime.now().plusDays(days));
            }
            case "unban" -> {
                user.setActive(true);
                user.setBanReason(null);
                user.setSuspendedUntil(null);
                user.setDeletedAt(null);
            }
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid action: " + request.action());
        }

        User saved = userRepository.save(user);
        adminOperationsService.log(authUser.userId(), "USER_MODERATED", "user", id.toString(),
                "action=" + action + (request.reason() != null ? ", reason=" + request.reason() : ""));
        return ok(userMapper.toResponse(saved));
    }

    @PostMapping("/users/bulk-moderate")
    @Transactional
    public ResponseEntity<Map<String, Object>> bulkModerateUsers(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody BulkModerateRequest request,
            @RequestHeader(value = "X-Step-Up-Token", required = false) String stepUpToken
    ) {
        requireAdmin(authUser);
        if (!authUser.role().canModerateUsers()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Support or Platform Admin role required for user moderation");
        }
        String action = request.action().trim().toLowerCase();
        if ("ban".equals(action) || "suspend".equals(action)) {
            adminStepUpService.requireStepUp(authUser, stepUpToken);
        }
        List<String> succeeded = new java.util.ArrayList<>();
        List<String> failed = new java.util.ArrayList<>();
        for (UUID id : request.userIds()) {
            try {
                if (authUser.userId().equals(id)) throw new ApiException(HttpStatus.BAD_REQUEST, "Admins cannot moderate their own account");
                User user = userRepository.findById(id)
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
                if (user.getRole().isAdmin()) throw new ApiException(HttpStatus.FORBIDDEN, "Cannot moderate admin accounts");
                switch (action) {
                    case "enable", "unban" -> {
                        user.setActive(true);
                        user.setBanReason(null);
                        user.setSuspendedUntil(null);
                        user.setDeletedAt(null);
                    }
                    case "disable" -> user.setActive(false);
                    case "ban" -> {
                        user.setActive(false);
                        user.setBanReason(request.reason());
                        user.setSuspendedUntil(null);
                        user.setDeletedAt(OffsetDateTime.now());
                    }
                    case "suspend" -> {
                        int days = request.suspendDays() != null && request.suspendDays() > 0 ? request.suspendDays() : 30;
                        user.setActive(false);
                        user.setBanReason(request.reason());
                        user.setSuspendedUntil(OffsetDateTime.now().plusDays(days));
                    }
                    default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid action: " + request.action());
                }
                userRepository.save(user);
                succeeded.add(id.toString());
            } catch (Exception ex) {
                failed.add(id.toString());
            }
        }
        adminOperationsService.log(authUser.userId(), "USERS_BULK_MODERATED", "user", String.join(",", succeeded),
                "action=" + action + ", succeeded=" + succeeded.size() + ", failed=" + failed.size());
        return ok(Map.of("succeeded", succeeded, "failed", failed));
    }

    @GetMapping("/brands/{brandId}/metrics")
    public ResponseEntity<Map<String, Object>> brandMetrics(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID brandId
    ) {
        requireAdmin(authUser);
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Brand not found"));
        List<Order> orders = orderRepository.findByBrandIdOrderByCreatedAtDesc(brandId);
        long totalOrders = orders.size();
        long completedOrders = orders.stream().filter(order -> order.getStatus() == OrderStatus.COMPLETED).count();
        long uniqueCreators = orders.stream().map(order -> order.getCreator().getId()).distinct().count();
        long repeatCreators = orders.stream()
                .collect(java.util.stream.Collectors.groupingBy(order -> order.getCreator().getId(), java.util.stream.Collectors.counting()))
                .values()
                .stream()
                .filter(count -> count > 1)
                .count();
        double completionRate = totalOrders == 0 ? 0.0 : Math.round((completedOrders * 1000.0 / totalOrders)) / 10.0;
        double repeatCreatorRate = uniqueCreators == 0 ? 0.0 : Math.round((repeatCreators * 1000.0 / uniqueCreators)) / 10.0;
        double avgRating = Math.round(reviewRepository.averageRatingByBrand(brandId) * 10.0) / 10.0;
        boolean missingVerification = brand.getBusinessVerificationStatus() == null
                || brand.getBusinessVerificationStatus().isBlank()
                || "rejected".equalsIgnoreCase(brand.getBusinessVerificationStatus());
        boolean flagged = missingVerification || (totalOrders >= 5 && completionRate < 60.0);
        String flagReason = missingVerification
                ? "Business verification is incomplete"
                : flagged ? "Completion rate is below operational target" : null;
        return ok(Map.of(
                "brandId", brandId,
                "totalOrders", totalOrders,
                "completedOrders", completedOrders,
                "completionRate", completionRate,
                "avgRating", avgRating,
                "repeatCreatorRate", repeatCreatorRate,
                "flaggedForReview", flagged,
                "flagReason", flagReason == null ? "" : flagReason
        ));
    }

    @GetMapping("/creators/{creatorId}/ambassador-score")
    public ResponseEntity<Map<String, Object>> creatorAmbassadorScore(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID creatorId
    ) {
        requireAdmin(authUser);
        creatorRepository.findById(creatorId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator not found"));
        AmbassadorScore score = ambassadorScoreRepository.findByCreatorId(creatorId)
                .orElseGet(() -> AmbassadorScore.builder().creatorId(creatorId).build());
        int nextTierPoints = Math.max(0, 100 - score.getTotal());
        return ok(Map.of(
                "creatorId", creatorId,
                "score", Map.of(
                        "total", score.getTotal(),
                        "deliveryScore", score.getDeliveryScore(),
                        "ratingScore", score.getRatingScore(),
                        "accountAgeScore", score.getAccountAgeScore(),
                        "cancellationScore", score.getCancellationScore(),
                        "profileCompletenessScore", score.getProfileCompletenessScore(),
                        "consistencyScore", score.getConsistencyScore()
                ),
                "tier", score.getTier().name().toLowerCase(),
                "percentileRank", score.getPercentileRank(),
                "strengths", score.getStrengths(),
                "improvements", score.getImprovements(),
                "nextTierPoints", nextTierPoints
        ));
    }

    @GetMapping("/api-logs")
    public ResponseEntity<Map<String, Object>> apiLogs(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int limit
    ) {
        requireAdmin(authUser);
        Boolean success = null;
        if ("success".equalsIgnoreCase(status)) success = true;
        if ("error".equalsIgnoreCase(status)) success = false;
        Page<ApiLog> result = apiLogRepository.search(blankToNull(service), success, PageRequest.of(safePage(page), safeLimit(limit)));
        return ok(Map.of(
                "logs", result.getContent().stream().map(this::toApiLogMap).toList(),
                "total", result.getTotalElements()
        ));
    }

    @GetMapping("/payments/withdrawals")
    public ResponseEntity<Map<String, Object>> listWithdrawals(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        requireAdmin(authUser);
        WithdrawalStatus statusFilter = null;
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("all")) {
            try {
                statusFilter = WithdrawalStatus.valueOf(status.trim().toUpperCase());
            } catch (Exception e) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid withdrawal status: " + status);
            }
        }
        Page<WithdrawalRequest> result = withdrawalService.listForAdmin(search, statusFilter, page, size);
        return ok(Map.of(
                "withdrawals", result.getContent().stream().map(this::toWithdrawalMap).toList(),
                "total", result.getTotalElements(),
                "page", result.getNumber(),
                "size", result.getSize()
        ));
    }

    @PatchMapping("/payments/withdrawals/{id}/status")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateWithdrawalStatus(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID id,
            @Valid @RequestBody WithdrawalStatusRequest request,
            @RequestHeader(value = "X-Step-Up-Token", required = false) String stepUpToken
    ) {
        requireAdmin(authUser);
        if (!authUser.role().canProcessPayments()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Finance Ops or Platform Admin role required");
        }
        adminStepUpService.requireStepUp(authUser, stepUpToken);
        WithdrawalStatus newStatus;
        try {
            newStatus = WithdrawalStatus.valueOf(request.status().trim().toUpperCase());
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid status: " + request.status());
        }
        WithdrawalRequest wr = withdrawalService.processWithdrawal(id, newStatus);
        adminOperationsService.log(authUser.userId(), "WITHDRAWAL_STATUS_CHANGED", "withdrawal_request",
                id.toString(), "status=" + newStatus.name().toLowerCase());
        return ok(toWithdrawalMap(wr));
    }

    private Map<String, Object> toWithdrawalMap(WithdrawalRequest wr) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", wr.getId());
        m.put("creatorId", wr.getCreator().getId());
        m.put("creatorName", wr.getCreator().getName());
        m.put("amount", wr.getAmount());
        m.put("status", wr.getStatus().name().toLowerCase());
        m.put("payoutMethodType", wr.getPayoutMethod() != null ? wr.getPayoutMethod().getType() : null);
        m.put("createdAt", wr.getCreatedAt());
        return m;
    }

    private ResponseEntity<Map<String, Object>> ok(Object data) {
        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    private Creator findCreator(UUID creatorId) {
        return creatorRepository.findById(creatorId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator not found"));
    }

    private Brand findBrand(UUID brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Brand not found"));
    }

    private String normalizeDocumentStatus(String rawStatus) {
        String status = rawStatus == null ? "" : rawStatus.trim().toUpperCase();
        if (!List.of("PENDING", "APPROVED", "REJECTED").contains(status)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "status must be pending, approved, or rejected");
        }
        return status;
    }

    private boolean hasAllRequiredApprovedDocuments(UUID brandId) {
        List<BrandVerificationDocument> docs = brandVerificationDocumentRepository.findByBrandIdOrderByUploadedAtDesc(brandId);
        return List.of("tax_id", "business_registration", "bank_details").stream()
                .allMatch(type -> docs.stream().anyMatch(doc ->
                        type.equalsIgnoreCase(doc.getType()) && "APPROVED".equalsIgnoreCase(doc.getStatus())
                ));
    }

    private List<String> requiredCreatorDocumentTypes() {
        return List.of("identity", "social_profile", "portfolio_sample");
    }

    private boolean hasAllRequiredApprovedCreatorDocuments(UUID creatorId) {
        List<CreatorVerificationDocument> docs = creatorVerificationDocumentRepository.findByCreatorIdOrderByUploadedAtDesc(creatorId);
        return requiredCreatorDocumentTypes().stream()
                .allMatch(type -> docs.stream().anyMatch(doc ->
                        type.equalsIgnoreCase(doc.getType()) && "APPROVED".equalsIgnoreCase(doc.getStatus())
                ));
    }

    private void logBrandVerificationEvent(Brand brand, BrandVerificationDocument document, UUID actorId, String eventType, String details) {
        User actor = userRepository.findById(actorId).orElse(null);
        brandVerificationEventRepository.save(BrandVerificationEvent.builder()
                .brand(brand)
                .document(document)
                .actor(actor)
                .eventType(eventType)
                .details(details)
                .build());
    }

    private void logCreatorVerificationEvent(Creator creator, CreatorVerificationDocument document, UUID actorId, String eventType, String details) {
        User actor = userRepository.findById(actorId).orElse(null);
        creatorVerificationEventRepository.save(CreatorVerificationEvent.builder()
                .creator(creator)
                .document(document)
                .actor(actor)
                .eventType(eventType)
                .details(details)
                .build());
    }

    private Map<String, Object> toVerificationDocumentMap(BrandVerificationDocument doc) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", doc.getId());
        row.put("type", doc.getType());
        row.put("fileName", doc.getFileName());
        row.put("fileUrl", doc.getFileUrl());
        row.put("status", doc.getStatus() == null ? "pending" : doc.getStatus().toLowerCase());
        row.put("rejectionReason", doc.getRejectionReason());
        row.put("uploadedAt", doc.getUploadedAt());
        row.put("reviewedAt", doc.getReviewedAt());
        row.put("reviewedBy", toUserSummary(doc.getReviewedBy()));
        return row;
    }

    private Map<String, Object> toVerificationEventMap(BrandVerificationEvent event) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", event.getId());
        row.put("eventType", event.getEventType());
        row.put("details", event.getDetails());
        row.put("createdAt", event.getCreatedAt());
        row.put("documentId", event.getDocument() == null ? null : event.getDocument().getId());
        row.put("actor", toUserSummary(event.getActor()));
        return row;
    }

    private Map<String, Object> toCreatorVerificationDocumentMap(CreatorVerificationDocument doc) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", doc.getId());
        row.put("type", doc.getType());
        row.put("fileName", doc.getFileName());
        row.put("fileUrl", doc.getFileUrl());
        row.put("status", doc.getStatus() == null ? "pending" : doc.getStatus().toLowerCase());
        row.put("rejectionReason", doc.getRejectionReason());
        row.put("uploadedAt", doc.getUploadedAt());
        row.put("reviewedAt", doc.getReviewedAt());
        row.put("reviewedBy", toUserSummary(doc.getReviewedBy()));
        return row;
    }

    private Map<String, Object> toCreatorVerificationEventMap(CreatorVerificationEvent event) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", event.getId());
        row.put("eventType", event.getEventType());
        row.put("details", event.getDetails());
        row.put("createdAt", event.getCreatedAt());
        row.put("documentId", event.getDocument() == null ? null : event.getDocument().getId());
        row.put("actor", toUserSummary(event.getActor()));
        return row;
    }

    private Map<String, Object> toUserSummary(User user) {
        if (user == null) return null;
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", user.getId());
        row.put("name", user.getName());
        row.put("email", user.getEmail());
        return row;
    }

    private Map<String, Object> toApiLogMap(ApiLog log) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", log.getId());
        row.put("timestamp", log.getCreatedAt());
        row.put("method", log.getMethod());
        row.put("path", log.getPath());
        row.put("statusCode", log.getStatusCode());
        row.put("durationMs", log.getDurationMs());
        row.put("service", log.getService());
        row.put("errorMessage", log.getErrorMessage());
        return row;
    }

    private void requireAdmin(AuthenticatedUser authUser) {
        if (authUser == null || !authUser.role().isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin access required");
        }
        // Note: framework-level role check in SecurityConfig enforces hasAnyRole before reaching here
    }

    private String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }

    private int safePage(int page) {
        return Math.max(page, 0);
    }

    private int safeLimit(int limit) {
        return Math.max(1, Math.min(limit, 100));
    }

    private UserRole parseRoleFilter(String role) {
        String value = blankToNull(role);
        if (value == null || value.equalsIgnoreCase("all")) return null;
        try {
            return UserRole.valueOf(value.trim().toUpperCase());
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid role: " + role);
        }
    }

    private OrderStatus parseOrderStatusFilter(String status) {
        String value = blankToNull(status);
        if (value == null || value.equalsIgnoreCase("all")) return null;
        try {
            return OrderStatus.valueOf(value.trim().toUpperCase());
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid status: " + status);
        }
    }

    private boolean matchesOrderSearch(Order order, String search) {
        if (search == null) return true;
        String lowered = search.toLowerCase();
        return Stream.of(
                        order.getId() == null ? null : order.getId().toString(),
                        order.getOrderNumber(),
                        order.getServicePackage() == null ? null : order.getServicePackage().getTitle(),
                        order.getServicePackage() == null ? null : order.getServicePackage().getName(),
                        order.getCreator() == null ? null : order.getCreator().getName(),
                        order.getCreator() == null ? null : order.getCreator().getEmail(),
                        order.getBrand() == null ? null : order.getBrand().getName(),
                        order.getBrand() == null ? null : order.getBrand().getEmail()
                )
                .filter(value -> value != null)
                .anyMatch(value -> value.toLowerCase().contains(lowered));
    }

    private Map<String, Object> toApplicationMap(AmbassadorApplication app) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", app.getId());
        row.put("creatorId", app.getCreator().getId());
        row.put("creatorName", app.getCreator().getName());
        row.put("creatorEmail", app.getCreator().getEmail());
        row.put("status", app.getStatus() == null ? null : app.getStatus().name().toLowerCase());
        row.put("submittedAt", app.getSubmittedAt());
        row.put("identityVerified", app.isIdentityVerified());
        row.put("engagementVerified", app.isEngagementVerified());
        row.put("contentReviewPassed", app.isContentReviewPassed());
        row.put("backgroundCheckPassed", app.isBackgroundCheckPassed());
        row.put("notes", app.getNotes());
        row.put("rejectionReason", app.getRejectionReason());
        row.put("approvedAt", app.getApprovedAt());
        row.put("createdAt", app.getCreatedAt());
        return row;
    }
}
