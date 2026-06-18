package com.chamcham.backend.controller;

import com.chamcham.backend.config.security.AuthenticatedUser;
import com.chamcham.backend.dto.order.OrderResponse;
import com.chamcham.backend.entity.AmbassadorApplication;
import com.chamcham.backend.entity.Brand;
import com.chamcham.backend.entity.Creator;
import com.chamcham.backend.entity.Order;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.entity.enums.OrderStatus;
import com.chamcham.backend.entity.enums.CreatorBadgeLevel;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.BrandMapper;
import com.chamcham.backend.mapper.CreatorMapper;
import com.chamcham.backend.mapper.OrderMapper;
import com.chamcham.backend.mapper.UserMapper;
import com.chamcham.backend.repository.BrandRepository;
import com.chamcham.backend.repository.CreatorRepository;
import com.chamcham.backend.repository.OrderRepository;
import com.chamcham.backend.repository.UserRepository;
import com.chamcham.backend.service.*;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
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
                           AdminStepUpService adminStepUpService) {
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
    }

    public record UserStatusRequest(@NotNull Boolean active) {}

    public record UserModerateRequest(
            @NotNull @NotBlank @Size(max = 20) String action,  // "suspend" | "ban" | "unban"
            @Size(max = 500) String reason,
            Integer suspendDays
    ) {}

    public record CreatorVerificationRequest(@NotNull Boolean verified) {}

    public record CreatorBadgeRequest(@NotNull String badgeLevel) {}

    public record BrandVerificationRequest(
            @Size(max = 50) String status,
            @Size(max = 255) String contactEmail,
            @Size(max = 50) String phoneNumber
    ) {}

    public record OrderStatusRequest(@NotNull String status) {}

    public record AmbassadorReviewRequest(
            @NotNull @Size(max = 30) String status,
            @Size(max = 2000) String notes
    ) {}

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
        creator.setVerified(request.verified());
        creator.setBadgeLevel(request.verified() ? CreatorBadgeLevel.VERIFIED : CreatorBadgeLevel.NONE);
        Creator saved = creatorRepository.save(creator);
        adminOperationsService.log(authUser.userId(), "CREATOR_VERIFICATION_CHANGED", "creator", id.toString(),
                "verified=" + request.verified() + ", badgeLevel=" + saved.getBadgeLevel().name().toLowerCase());
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
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Brand not found"));
        if (request.status() != null) brand.setBusinessVerificationStatus(request.status());
        if (request.contactEmail() != null) brand.setVerificationContactEmail(request.contactEmail());
        if (request.phoneNumber() != null) brand.setVerificationPhoneNumber(request.phoneNumber());
        Brand saved = brandRepository.save(brand);
        adminOperationsService.log(authUser.userId(), "BRAND_VERIFICATION_CHANGED", "brand", id.toString(), "status=" + saved.getBusinessVerificationStatus());
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
                        : verificationStatus.trim().toLowerCase(),
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
                    com.chamcham.backend.entity.enums.AmbassadorAppStatus.valueOf(request.status().trim().toUpperCase()),
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
