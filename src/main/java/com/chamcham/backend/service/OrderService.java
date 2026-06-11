package com.chamcham.backend.service;

import com.chamcham.backend.dto.order.OrderResponse;
import com.chamcham.backend.entity.Brand;
import com.chamcham.backend.entity.Deliverable;
import com.chamcham.backend.entity.Order;
import com.chamcham.backend.entity.ServicePackage;
import com.chamcham.backend.entity.Transaction;
import com.chamcham.backend.entity.enums.DealType;
import com.chamcham.backend.entity.enums.OrderStatus;
import com.chamcham.backend.entity.enums.TransactionStatus;
import com.chamcham.backend.entity.enums.TransactionType;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.OrderMapper;
import com.chamcham.backend.repository.BrandRepository;
import com.chamcham.backend.repository.OrderRepository;
import com.chamcham.backend.repository.ServicePackageRepository;
import com.chamcham.backend.repository.TransactionRepository;
import com.chamcham.backend.repository.WalletRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
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
            OrderStatus.DELIVERED,  EnumSet.of(OrderStatus.REVIEW, OrderStatus.COMPLETED),
            OrderStatus.REVIEW,     EnumSet.of(OrderStatus.COMPLETED, OrderStatus.REVISION),
            OrderStatus.REVISION,   EnumSet.of(OrderStatus.IN_PROGRESS),
            OrderStatus.COMPLETED,  EnumSet.noneOf(OrderStatus.class),
            OrderStatus.CANCELLED,  EnumSet.noneOf(OrderStatus.class)
    );

    private final OrderRepository orderRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final BrandRepository brandRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final OrderMapper orderMapper;
    public OrderService(OrderRepository orderRepository,
                        ServicePackageRepository servicePackageRepository,
                        BrandRepository brandRepository,
                        WalletRepository walletRepository,
                        TransactionRepository transactionRepository,
                        OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.brandRepository = brandRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.orderMapper = orderMapper;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(UUID userId, UserRole role) {
        List<Order> orders = role.isCreator()
                ? orderRepository.findByCreatorIdOrderByCreatedAtDesc(userId)
                : orderRepository.findByBrandIdOrderByCreatedAtDesc(userId);
        return orders.stream().map(orderMapper::toResponse).toList();
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
        return createOrder(packageId, brandUserId, role, amount, barterDetails, message, dealType, false);
    }

    @Transactional
    public OrderResponse createPrivateDealOrder(UUID packageId, UUID brandUserId, Integer amount,
                                                String barterDetails, String message, DealType dealType) {
        return createOrder(packageId, brandUserId, UserRole.BRAND, amount, barterDetails, message, dealType, true);
    }

    private OrderResponse createOrder(UUID packageId, UUID brandUserId, UserRole role,
                                      Integer amount, String barterDetails, String message,
                                      DealType dealType, boolean allowPrivatePackage) {
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
                .deadlineDate(LocalDate.now().plusDays(Math.max(pkg.getDeliveryDays(), 1)))
                .build();

        List<Deliverable> deliverables = packageDeliverables(pkg).stream()
                .map(name -> Deliverable.builder()
                        .order(order)
                        .name(name)
                        .status(Deliverable.DeliverableStatus.PENDING)
                        .build())
                .toList();
        order.setDeliverables(deliverables);

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse updateStatus(UUID orderId, OrderStatus newStatus, UUID userId, UserRole role) {
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
            case CANCELLED -> {
                if (role.isAdmin()) break;
                if (role.isBrand() && order.getStatus() != OrderStatus.PENDING) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Brand can only cancel PENDING orders");
                }
            }
            default -> { /* permit admin */ }
        }

        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(order.getStatus(), EnumSet.noneOf(OrderStatus.class));
        if (!allowed.contains(newStatus)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Cannot transition from " + order.getStatus() + " to " + newStatus);
        }

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);
        if (newStatus == OrderStatus.COMPLETED) {
            releaseCreatorEarnings(saved);
        }
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

    private void validateDealPayload(DealType dealType, Integer amount, String barterDetails) {
        if ((dealType == DealType.PAID || dealType == DealType.paid
                || dealType == DealType.HYBRID || dealType == DealType.hybrid)
                && (amount == null || amount <= 0)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "amount is required for paid and hybrid orders");
        }
        if ((dealType == DealType.BARTER || dealType == DealType.barter
                || dealType == DealType.HYBRID || dealType == DealType.hybrid)
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

    private void releaseCreatorEarnings(Order order) {
        int amount = order.getAmount() == null ? 0 : order.getAmount();
        if (amount <= 0 || order.getDealType() == DealType.BARTER) {
            return;
        }
        if (transactionRepository.existsByOrderIdAndType(order.getId(), TransactionType.EARNING)) {
            return;
        }

        walletRepository.creditCreatorEarnings(order.getCreator().getId(), amount);

        String orderLabel = order.getOrderNumber() == null ? order.getId().toString() : order.getOrderNumber();
        transactionRepository.save(Transaction.builder()
                .creator(order.getCreator())
                .order(order)
                .type(TransactionType.EARNING)
                .amount(amount)
                .description("Order " + orderLabel + " payout credit")
                .status(TransactionStatus.COMPLETED)
                .build());
    }
}
