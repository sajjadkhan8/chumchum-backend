package com.chamcham.backend.service;

import com.chamcham.backend.dto.order.OrderResponse;
import com.chamcham.backend.entity.Brand;
import com.chamcham.backend.entity.Order;
import com.chamcham.backend.entity.ServicePackage;
import com.chamcham.backend.entity.enums.OrderStatus;
import com.chamcham.backend.entity.enums.PackagePricingType;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.OrderMapper;
import com.chamcham.backend.repository.BrandRepository;
import com.chamcham.backend.repository.OrderRepository;
import com.chamcham.backend.repository.ServicePackageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final BrandRepository brandRepository;
    private final OrderMapper orderMapper;
    private final AtomicLong orderNumberCounter = new AtomicLong(1);

    public OrderService(
            OrderRepository orderRepository,
            ServicePackageRepository servicePackageRepository,
            BrandRepository brandRepository,
            OrderMapper orderMapper
    ) {
        this.orderRepository = orderRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.brandRepository = brandRepository;
        this.orderMapper = orderMapper;
    }

    public List<OrderResponse> getOrders(UUID userId) {
        return orderRepository.findCompletedByParticipant(userId)
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    public OrderResponse createOrder(UUID packageId, UUID brandId, UserRole role, BigDecimal amount, String barterDetails) {
        if (!role.isBrand() && !role.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only brands can place orders");
        }

        ServicePackage servicePackage = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Package not found"));
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Brand profile not found"));

        String orderNumber = "ORD-" + String.format("%05d", orderNumberCounter.getAndIncrement());

        Order order = Order.builder()
                .id(UUID.randomUUID())
                .orderNumber(orderNumber)
                .servicePackage(servicePackage)
                .image(servicePackage.getCoverImage())
                .title(servicePackage.getTitle())
                .brand(brand)
                .creator(servicePackage.getCreator())
                .dealType(PackagePricingType.PAID)
                .amount(amount)
                .barterDetails(barterDetails)
                .status(OrderStatus.PENDING)
                .progress(0)
                .build();

        orderRepository.save(order);
        return orderMapper.toResponse(order);
    }

    public void updateOrderStatus(UUID orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
        order.setStatus(status);
        orderRepository.save(order);
    }
}
