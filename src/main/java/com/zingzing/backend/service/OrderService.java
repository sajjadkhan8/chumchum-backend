package com.zingzing.backend.service;

import com.zingzing.backend.dto.order.OrderResponse;
import com.zingzing.backend.dto.order.DeliverableResponse;
import com.zingzing.backend.config.CommerceProperties;
import com.zingzing.backend.entity.Brand;
import com.zingzing.backend.entity.Creator;
import com.zingzing.backend.entity.Deliverable;
import com.zingzing.backend.entity.Order;
import com.zingzing.backend.entity.ServicePackage;
import com.zingzing.backend.entity.Transaction;
import com.zingzing.backend.entity.enums.DealType;
import com.zingzing.backend.entity.enums.OrderStatus;
import com.zingzing.backend.entity.enums.TransactionStatus;
import com.zingzing.backend.entity.enums.TransactionType;
import com.zingzing.backend.entity.enums.UserRole;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.mapper.OrderMapper;
import com.zingzing.backend.repository.BrandRepository;
import com.zingzing.backend.repository.BrandWalletRepository;
import com.zingzing.backend.repository.CreatorRepository;
import com.zingzing.backend.repository.DeliverableRepository;
import com.zingzing.backend.repository.OrderRepository;
import com.zingzing.backend.repository.ServicePackageRepository;
import com.zingzing.backend.repository.TransactionRepository;
import com.zingzing.backend.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class OrderService {

    private static final Set<OrderStatus> ACTIVE_STATUSES = EnumSet.of(
            OrderStatus.ACCEPTED, OrderStatus.IN_PROGRESS,
            OrderStatus.DELIVERED, OrderStatus.REVIEW, OrderStatus.REVISION);

    /** Allowed status transitions per spec workflow. */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING,    EnumSet.of(OrderStatus.ACCEPTED, OrderStatus.CANCELLED),
            OrderStatus.ACCEPTED,   EnumSet.of(OrderStatus.IN_PROGRESS, OrderStatus.CANCELLED),
            OrderStatus.IN_PROGRESS,EnumSet.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED,  EnumSet.of(OrderStatus.REVIEW, OrderStatus.REVISION, OrderStatus.COMPLETED),
            OrderStatus.REVIEW,     EnumSet.of(OrderStatus.COMPLETED, OrderStatus.REVISION),
            OrderStatus.REVISION,   EnumSet.of(OrderStatus.IN_PROGRESS),
            OrderStatus.COMPLETED,  EnumSet.noneOf(OrderStatus.class),
            OrderStatus.CANCELLED,  EnumSet.noneOf(OrderStatus.class)
    );

    private final OrderRepository orderRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final BrandRepository brandRepository;
    private final CreatorRepository creatorRepository;
    private final DeliverableRepository deliverableRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final OrderMapper orderMapper;
    private final NotificationService notificationService;
    private final AffiliateService affiliateService;
    private final BrandWalletRepository brandWalletRepository;
    private final PackageAnalyticsTrackingService packageAnalyticsTrackingService;
    private final CommerceProperties commerceProperties;
    private final double feeRate;

    public OrderService(OrderRepository orderRepository,
                        ServicePackageRepository servicePackageRepository,
                        BrandRepository brandRepository,
                        CreatorRepository creatorRepository,
                        DeliverableRepository deliverableRepository,
                        WalletRepository walletRepository,
                        TransactionRepository transactionRepository,
                        OrderMapper orderMapper,
                        NotificationService notificationService,
                        AffiliateService affiliateService,
                        BrandWalletRepository brandWalletRepository,
                        PackageAnalyticsTrackingService packageAnalyticsTrackingService,
                        CommerceProperties commerceProperties,
                        @Value("${platform.fee-rate:0.10}") double feeRate) {
        this.orderRepository = orderRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.brandRepository = brandRepository;
        this.creatorRepository = creatorRepository;
        this.deliverableRepository = deliverableRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.orderMapper = orderMapper;
        this.notificationService = notificationService;
        this.affiliateService = affiliateService;
        this.brandWalletRepository = brandWalletRepository;
        this.packageAnalyticsTrackingService = packageAnalyticsTrackingService;
        this.commerceProperties = commerceProperties;
        this.feeRate = feeRate;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOrders(UUID userId, UserRole role, int page, int limit,
                                         String status, String search) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(limit, 1), 100));
        OrderStatus parsedStatus = null;
        if (status != null && !status.isBlank()) {
            try { parsedStatus = OrderStatus.valueOf(status.trim().toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        String searchParam = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<Order> orderPage = role.isCreator()
                ? orderRepository.findByCreatorIdFiltered(userId, parsedStatus, searchParam, pageable)
                : orderRepository.findByBrandIdFiltered(userId, parsedStatus, searchParam, pageable);
        Map<String, Object> result = new HashMap<>();
        result.put("orders", orderPage.getContent().stream().map(orderMapper::toResponse).toList());
        result.put("total", orderPage.getTotalElements());
        result.put("page", page);
        result.put("limit", limit);
        return result;
    }

    public Map<String, Object> checkAndInitiatePayment(UUID brandId, int amountPkr,
                                                        SafepayService safepayService) {
        com.zingzing.backend.entity.BrandWallet wallet = brandWalletRepository.findById(brandId)
                .orElse(null);
        long balance = wallet != null ? wallet.getWalletBalance() : 0L;
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        if (balance >= amountPkr) {
            result.put("walletSufficient", true);
            result.put("balance", balance);
            result.put("required", amountPkr);
            result.put("checkoutRequired", false);
            return result;
        }
        int topUpAmount = (int) Math.max(amountPkr - balance, commerceProperties.getMinimumCashAmountPkr());
        SafepayService.CheckoutSessionResponse session =
                safepayService.initiateWalletTopUp(brandId, topUpAmount);
        result.put("walletSufficient", false);
        result.put("balance", balance);
        result.put("required", amountPkr);
        result.put("checkoutRequired", true);
        result.put("topUpAmount", topUpAmount);
        result.put("sessionId", session.sessionId());
        result.put("checkoutUrl", session.checkoutUrl());
        result.put("expiresAt", session.expiresAt());
        return result;
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId, UUID userId, UserRole role) {
        Order order = findOrder(orderId);
        if (!role.isAdmin()
                && !order.getCreator().getId().equals(userId)
                && !order.getBrand().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse createOrder(UUID packageId, UUID brandUserId, UserRole role,
                                     Integer amount, String barterDetails, String message,
                                     DealType dealType) {
        return createOrder(packageId, brandUserId, role, amount, barterDetails, message, dealType, false, null);
    }

    @Transactional
    public OrderResponse createOrder(UUID packageId, UUID brandUserId, UserRole role,
                                     Integer amount, String barterDetails, String message,
                                     DealType dealType, String idempotencyKey) {
        return createOrder(packageId, brandUserId, role, amount, barterDetails, message, dealType, false, idempotencyKey);
    }

    @Transactional
    public OrderResponse createPrivateDealOrder(UUID packageId, UUID brandUserId, Integer amount,
                                                String barterDetails, String message, DealType dealType) {
        return createOrder(packageId, brandUserId, UserRole.BRAND, amount, barterDetails, message, dealType, true, null);
    }

    private OrderResponse createOrder(UUID packageId, UUID brandUserId, UserRole role,
                                      Integer amount, String barterDetails, String message,
                                      DealType dealType, boolean allowPrivatePackage, String idempotencyKey) {
        // Idempotency: return the existing order if this key was already processed
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Order> existing = orderRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) {
                return orderMapper.toResponse(existing.get());
            }
        }

        if (!role.isBrand() && !role.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only brands can place orders");
        }
        ServicePackage pkg = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Package not found"));
        if (!allowPrivatePackage && "private".equalsIgnoreCase(pkg.getVisibility())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Private deal packages cannot be ordered directly");
        }
        Brand brand = brandRepository.findById(brandUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Brand profile not found"));

        DealType effectiveDealType = dealType != null ? dealType : DealType.PAID;
        validateDealPayload(effectiveDealType, amount, barterDetails);

        if ((effectiveDealType == DealType.PAID || effectiveDealType == DealType.HYBRID)
                && amount != null && amount > 0) {
            int held = brandWalletRepository.holdEscrow(brand.getId(), amount);
            if (held == 0) {
                throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "Insufficient wallet balance to place this order");
            }
        }

        String orderNumber = "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        Order order = Order.builder()
                .id(UUID.randomUUID())
                .orderNumber(orderNumber)
                .servicePackage(pkg)
                .brand(brand)
                .creator(pkg.getCreator())
                .dealType(effectiveDealType)
                .amount(amount)
                .barterDetails(barterDetails)
                .message(message)
                .status(OrderStatus.PENDING)
                .progress(0)
                .deadlineDate(OffsetDateTime.now(ZoneId.of("Asia/Karachi"))
                        .toLocalDate().plusDays(Math.max(pkg.getDeliveryDays(), 1))
                        .atTime(23, 59, 59).atZone(ZoneId.of("Asia/Karachi")).toOffsetDateTime())
                .barterExpectedBy((effectiveDealType == DealType.BARTER || effectiveDealType == DealType.HYBRID)
                        ? OffsetDateTime.now(ZoneId.of("Asia/Karachi")).plusDays(14) : null)
                .idempotencyKey(idempotencyKey != null && !idempotencyKey.isBlank() ? idempotencyKey.trim() : null)
                .build();

        List<Deliverable> deliverables = packageDeliverables(pkg).stream()
                .map(name -> Deliverable.builder()
                        .order(order)
                        .name(name)
                        .status(Deliverable.DeliverableStatus.PENDING)
                        .build())
                .toList();
        order.setDeliverables(deliverables);

        Order saved = orderRepository.save(order);
        packageAnalyticsTrackingService.track(pkg, "INQUIRY", brand, brand, "order_created", "{\"orderId\":\"" + saved.getId() + "\"}");
        notificationService.send(saved.getCreator().getId(), "order_placed", "New order received",
                "You have a new order for: " + pkg.getTitle(), "order", saved.getId());
        return orderMapper.toResponse(saved);
    }

    @Transactional
    public OrderResponse updateStatus(UUID orderId, OrderStatus newStatus, UUID userId, UserRole role, String message) {
        Order order = findOrder(orderId);
        requireParticipant(order, userId, role);

        // Role-based transition guards
        switch (newStatus) {
            case ACCEPTED, IN_PROGRESS, DELIVERED -> {
                if (!role.isCreator() && !role.isAdmin()) {
                    throw new ApiException(HttpStatus.FORBIDDEN, "Only creators can set status: " + newStatus);
                }
            }
            case COMPLETED, REVISION -> {
                if (!role.isBrand() && !role.isAdmin()) {
                    throw new ApiException(HttpStatus.FORBIDDEN, "Only brands can set status: " + newStatus);
                }
            }
            case REVIEW -> {
                if (!role.isAdmin()) {
                    throw new ApiException(HttpStatus.FORBIDDEN, "Review status is managed from deliverable approvals");
                }
            }
            case CANCELLED -> {
                if (role.isAdmin()) break;
                if (!role.isBrand() && !role.isCreator()) {
                    throw new ApiException(HttpStatus.FORBIDDEN, "Only participants can cancel orders");
                }
                if (order.getStatus() != OrderStatus.PENDING) {
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                            role.isCreator()
                                    ? "Creators can only decline PENDING orders"
                                    : "Brand can only cancel PENDING orders");
                }
            }
            default -> { /* permit admin */ }
        }

        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(order.getStatus(), EnumSet.noneOf(OrderStatus.class));
        if (!allowed.contains(newStatus)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Cannot transition from " + order.getStatus() + " to " + newStatus);
        }

        validateDeliverableTransition(order, newStatus);
        order.setStatus(newStatus);
        if (newStatus == OrderStatus.IN_PROGRESS && order.getProgress() == 0) {
            order.setProgress(5);
        } else if (newStatus == OrderStatus.COMPLETED) {
            order.setProgress(100);
        }
        Order saved = orderRepository.save(order);
        if (newStatus == OrderStatus.COMPLETED) {
            releaseCreatorEarnings(saved);
            Creator creator = saved.getCreator();
            creator.setCompletedDeals(creator.getCompletedDeals() + 1);
            creatorRepository.save(creator);
        }
        if (newStatus == OrderStatus.CANCELLED) {
            refundEscrowIfApplicable(saved);
        }
        notifyStatusChange(saved, newStatus, message);
        return orderMapper.toResponse(saved);
    }

    @Transactional
    public DeliverableResponse submitDeliverable(UUID orderId, UUID deliverableId, String fileUrl, String note,
                                                 UUID userId, UserRole role) {
        Order order = findOrder(orderId);
        if (!role.isAdmin() && (!role.isCreator() || !order.getCreator().getId().equals(userId))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the order creator can submit deliverables");
        }
        if (order.getStatus() != OrderStatus.IN_PROGRESS && order.getStatus() != OrderStatus.REVISION) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Deliverables can only be submitted while work is in progress or revision");
        }
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "fileUrl is required");
        }
        String requiredPrefix = "/api/v1/files/deliverables/" + orderId + "/" + deliverableId + "/";
        if (!fileUrl.startsWith(requiredPrefix) || fileUrl.substring(requiredPrefix.length()).contains("/")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Upload the file for this deliverable before submitting it");
        }

        Deliverable deliverable = findOrderDeliverable(orderId, deliverableId);
        if (!hasStatus(deliverable, Deliverable.DeliverableStatus.PENDING,
                Deliverable.DeliverableStatus.IN_PROGRESS, Deliverable.DeliverableStatus.REVISION)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This deliverable is not awaiting a submission");
        }

        deliverable.setFileUrl(fileUrl.trim());
        deliverable.setStatus(Deliverable.DeliverableStatus.REVIEW);
        deliverable.setSubmittedAt(OffsetDateTime.now());
        Deliverable saved = deliverableRepository.save(deliverable);

        List<Deliverable> deliverables = deliverableRepository.findByOrderId(orderId);
        updateDerivedProgress(order, deliverables);
        if (deliverables.stream().allMatch(this::isSubmittedOrApproved)) {
            order.setStatus(OrderStatus.DELIVERED);
        }
        orderRepository.save(order);

        String detail = note == null || note.isBlank() ? saved.getName() : saved.getName() + ": " + note.trim();
        notificationService.send(order.getBrand().getId(), "deliverable_submitted", "Deliverable submitted",
                detail, "order", order.getId());
        return toDeliverableResponse(saved);
    }

    @Transactional
    public DeliverableResponse reviewDeliverable(UUID orderId, UUID deliverableId, String rawStatus, String comment,
                                                 UUID userId, UserRole role) {
        Order order = findOrder(orderId);
        if (!role.isAdmin() && (!role.isBrand() || !order.getBrand().getId().equals(userId))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the order brand can review deliverables");
        }
        if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.REVIEW) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Deliverables can only be reviewed after delivery");
        }

        Deliverable.DeliverableStatus requested;
        try {
            requested = Deliverable.DeliverableStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid status: " + rawStatus);
        }
        if (requested != Deliverable.DeliverableStatus.COMPLETED
                && requested != Deliverable.DeliverableStatus.REVISION) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Brand review can only approve or request revision");
        }

        Deliverable deliverable = findOrderDeliverable(orderId, deliverableId);
        if (!hasStatus(deliverable, Deliverable.DeliverableStatus.REVIEW)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This deliverable is not awaiting review");
        }
        deliverable.setStatus(requested);
        if (requested == Deliverable.DeliverableStatus.REVISION && comment != null && !comment.isBlank()) {
            deliverable.setRevisionNote(comment.trim());
        }
        Deliverable saved = deliverableRepository.save(deliverable);

        List<Deliverable> deliverables = deliverableRepository.findByOrderId(orderId);
        updateDerivedProgress(order, deliverables);
        if (requested == Deliverable.DeliverableStatus.REVISION) {
            order.setStatus(OrderStatus.REVISION);
        } else if (deliverables.stream().allMatch(this::isApproved)) {
            order.setStatus(OrderStatus.REVIEW);
        }
        orderRepository.save(order);

        String notifyBody = saved.getName();
        if (requested == Deliverable.DeliverableStatus.REVISION && comment != null && !comment.isBlank()) {
            notifyBody = saved.getName() + ": " + comment.trim();
        }
        notificationService.send(order.getCreator().getId(),
                requested == Deliverable.DeliverableStatus.REVISION ? "deliverable_revision" : "deliverable_approved",
                requested == Deliverable.DeliverableStatus.REVISION ? "Revision requested" : "Deliverable approved",
                notifyBody, "order", order.getId());
        return toDeliverableResponse(saved);
    }

    @Transactional
    public OrderResponse confirmBarterReceipt(UUID orderId, UUID brandUserId, UserRole role) {
        Order order = findOrder(orderId);
        if (!role.isBrand() && !role.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only brands can confirm barter receipt");
        }
        if (!role.isAdmin() && !order.getBrand().getId().equals(brandUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        if (order.getDealType() != DealType.BARTER && order.getDealType() != DealType.HYBRID) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Barter confirmation is only applicable to barter or hybrid orders");
        }
        order.setBarterProductReceived(true);
        Order saved = orderRepository.save(order);
        notificationService.send(order.getCreator().getId(), "barter_received",
                "Barter product received",
                "Brand has confirmed receipt of the barter product for order " + order.getOrderNumber(),
                "order", order.getId());
        return orderMapper.toResponse(saved);
    }

    @Transactional
    public OrderResponse updateProgress(UUID orderId, int progress, UUID userId) {
        Order order = findOrder(orderId);
        if (!order.getCreator().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        if (progress < 0 || progress > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Progress must be 0-100");
        }
        order.setProgress(progress);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    private Order findOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private void requireParticipant(Order order, UUID userId, UserRole role) {
        if (role.isAdmin()) return;
        if (!order.getCreator().getId().equals(userId) && !order.getBrand().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    private Deliverable findOrderDeliverable(UUID orderId, UUID deliverableId) {
        Deliverable deliverable = deliverableRepository.findById(deliverableId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Deliverable not found"));
        if (!deliverable.getOrder().getId().equals(orderId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Deliverable does not belong to this order");
        }
        return deliverable;
    }

    private void validateDeliverableTransition(Order order, OrderStatus newStatus) {
        List<Deliverable> deliverables = deliverableRepository.findByOrderId(order.getId());
        if (newStatus == OrderStatus.DELIVERED && !deliverables.stream().allMatch(this::isSubmittedOrApproved)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Submit every deliverable before marking the order delivered");
        }
        if (newStatus == OrderStatus.COMPLETED && !deliverables.stream().allMatch(this::isApproved)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Approve every deliverable before completing the order");
        }
        if (newStatus == OrderStatus.REVISION
                && deliverables.stream().noneMatch(deliverable -> hasStatus(deliverable, Deliverable.DeliverableStatus.REVISION))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Request revision on a deliverable first");
        }
    }

    private void updateDerivedProgress(Order order, List<Deliverable> deliverables) {
        if (deliverables.isEmpty()) {
            order.setProgress(0);
            return;
        }
        long submitted = deliverables.stream().filter(this::isSubmittedOrApproved).count();
        order.setProgress((int) Math.round(submitted * 100.0 / deliverables.size()));
    }

    private boolean isSubmittedOrApproved(Deliverable deliverable) {
        return hasStatus(deliverable, Deliverable.DeliverableStatus.REVIEW,
                Deliverable.DeliverableStatus.COMPLETED, Deliverable.DeliverableStatus.APPROVED);
    }

    private boolean isApproved(Deliverable deliverable) {
        return hasStatus(deliverable, Deliverable.DeliverableStatus.COMPLETED, Deliverable.DeliverableStatus.APPROVED);
    }

    private boolean hasStatus(Deliverable deliverable, Deliverable.DeliverableStatus... statuses) {
        String actual = deliverable.getStatus().name();
        for (Deliverable.DeliverableStatus status : statuses) {
            if (actual.equalsIgnoreCase(status.name())) return true;
        }
        return false;
    }

    private DeliverableResponse toDeliverableResponse(Deliverable deliverable) {
        return new DeliverableResponse(deliverable.getId(), deliverable.getOrder().getId(), deliverable.getName(),
                deliverable.getStatus().name().toLowerCase(), deliverable.getFileUrl(),
                deliverable.getSubmittedAt(), deliverable.getRevisionNote(), deliverable.getCreatedAt());
    }

    private void notifyStatusChange(Order order, OrderStatus newStatus, String message) {
        String body = "Order " + order.getOrderNumber() + " is now " + newStatus.name().toLowerCase().replace('_', ' ');
        if (newStatus == OrderStatus.CANCELLED && message != null && !message.isBlank()) {
            body = body + ". Note: " + message.trim();
        }
        boolean notifyBoth = newStatus == OrderStatus.COMPLETED || newStatus == OrderStatus.CANCELLED;
        if (notifyBoth) {
            notificationService.send(order.getCreator().getId(), "order_status", "Order status updated", body, "order", order.getId());
            notificationService.send(order.getBrand().getId(), "order_status", "Order status updated", body, "order", order.getId());
            return;
        }
        UUID recipientId = newStatus == OrderStatus.ACCEPTED || newStatus == OrderStatus.IN_PROGRESS
                || newStatus == OrderStatus.DELIVERED ? order.getBrand().getId() : order.getCreator().getId();
        notificationService.send(recipientId, "order_status", "Order status updated", body, "order", order.getId());
    }

    private void validateDealPayload(DealType dealType, Integer amount, String barterDetails) {
        if ((dealType == DealType.PAID || dealType == DealType.HYBRID)
                && (amount == null || amount <= 0)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "amount is required for paid and hybrid orders");
        }
        if ((dealType == DealType.BARTER || dealType == DealType.HYBRID)
                && (barterDetails == null || barterDetails.isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "barterDetails is required for barter and hybrid orders");
        }
    }

    private List<String> packageDeliverables(ServicePackage pkg) {
        List<String> deliverables = pkg.getDeliverables();
        if (deliverables == null || deliverables.isEmpty()) {
            return List.of("Package deliverables");
        }
        List<String> normalized = deliverables.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
        return normalized.isEmpty() ? List.of("Package deliverables") : normalized;
    }

    /**
     * Called by dispute resolution when the creator wins the dispute (CREATOR_FAVORED).
     * Idempotent — re-entrant if an EARNING transaction already exists for this order.
     */
    @Transactional
    public void releaseEarningsForDisputeResolution(Order order) {
        releaseCreatorEarnings(order);
    }

    private void releaseCreatorEarnings(Order order) {
        int amount = order.getAmount() == null ? 0 : order.getAmount();
        if (amount <= 0 || order.getDealType() == DealType.BARTER) {
            return;
        }
        if (transactionRepository.existsByOrderIdAndType(order.getId(), TransactionType.EARNING)) {
            return;
        }

        int fee = (int) Math.round(amount * feeRate);
        int creatorNet = amount - fee;

        walletRepository.creditCreatorEarnings(order.getCreator().getId(), creatorNet);
        brandWalletRepository.releaseEscrow(order.getBrand().getId(), amount);

        String orderLabel = order.getOrderNumber() == null ? order.getId().toString() : order.getOrderNumber();
        transactionRepository.save(Transaction.builder()
                .creator(order.getCreator())
                .order(order)
                .type(TransactionType.EARNING)
                .amount(creatorNet)
                .description("Order " + orderLabel + " payout credit")
                .status(TransactionStatus.COMPLETED)
                .build());
        if (fee > 0) {
            transactionRepository.save(Transaction.builder()
                    .creator(order.getCreator())
                    .order(order)
                    .type(TransactionType.PLATFORM_FEE)
                    .amount(fee)
                    .description("Order " + orderLabel + " platform fee")
                    .status(TransactionStatus.COMPLETED)
                    .build());
        }
        affiliateService.releaseCommissionForCompletedOrder(order);
    }

    private void refundEscrowIfApplicable(Order order) {
        int amount = order.getAmount() == null ? 0 : order.getAmount();
        if (amount <= 0 || order.getDealType() == DealType.BARTER) {
            return;
        }
        brandWalletRepository.refundEscrow(order.getBrand().getId(), amount);
    }
}
