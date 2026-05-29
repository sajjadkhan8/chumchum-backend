package com.chamcham.backend.service;

import com.chamcham.backend.dto.order.OrderResponse;
import com.chamcham.backend.entity.Brand;
import com.chamcham.backend.entity.Order;
import com.chamcham.backend.entity.ServicePackage;
import com.chamcham.backend.entity.enums.DealType;
import com.chamcham.backend.entity.enums.OrderStatus;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.OrderMapper;
import com.chamcham.backend.repository.BrandRepository;
import com.chamcham.backend.repository.OrderRepository;
import com.chamcham.backend.repository.ServicePackageRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

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
    private final OrderMapper orderMapper;
    private final AtomicLong orderNumberCounter = new AtomicLong(1000);

    public OrderService(OrderRepository orderRepository,
                        ServicePackageRepository servicePackageRepository,
                        BrandRepository brandRepository,
                        OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.brandRepository = brandRepository;
        this.orderMapper = orderMapper;
    }

    public List<OrderResponse> getOrders(UUID userId, UserRole role) {
        List<Order> orders = role.isCreator()
                ? orderRepository.findByCreatorIdOrderByCreatedAtDesc(userId)
                : orderRepository.findByBrandIdOrderByCreatedAtDesc(userId);
        return orders.stream().map(orderMapper::toResponse).toList();
    }

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
        if (!role.isBrand() && !role.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only brands can place orders");
        }
        ServicePackage pkg = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Package not found"));
        Brand brand = brandRepository.findById(brandUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Brand profile not found"));

        DealType effectiveDealType = dealType != null ? dealType : DealType.PAID;
        String orderNumber = "ORD-" + String.format("%06d", orderNumberCounter.getAndIncrement());

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
                .build();

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse updateStatus(UUID orderId, OrderStatus newStatus, UUID userId, UserRole role) {
        Order order = findOrder(orderId);

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
        return orderMapper.toResponse(orderRepository.save(order));
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
}
