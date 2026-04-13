package com.chamcham.backend.service;

import com.chamcham.backend.dto.order.OrderResponse;
import com.chamcham.backend.dto.order.PaymentIntentResponse;
import com.chamcham.backend.entity.Order;
import com.chamcham.backend.entity.ServicePackage;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.entity.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.OrderMapper;
import com.chamcham.backend.repository.OrderRepository;
import com.chamcham.backend.repository.ServicePackageRepository;
import com.chamcham.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;

    public OrderService(
            OrderRepository orderRepository,
            ServicePackageRepository servicePackageRepository,
            UserRepository userRepository,
            OrderMapper orderMapper
    ) {
        this.orderRepository = orderRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.userRepository = userRepository;
        this.orderMapper = orderMapper;
    }

    public List<OrderResponse> getOrders(UUID userId) {
        return orderRepository.findCompletedByParticipant(userId)
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    public PaymentIntentResponse createPaymentIntent(UUID packageId, UUID brandId, UserRole role) {
        if (!role.isBrand() && !role.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only brands can place orders");
        }

        ServicePackage servicePackage = servicePackageRepository.findById(packageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Package not found"));
        User brand = userRepository.findById(brandId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        String paymentIntent = "pi_" + UUID.randomUUID();
        String clientSecret = paymentIntent + "_secret";

        Order order = Order.builder()
                .id(UUID.randomUUID())
                .servicePackage(servicePackage)
                .image(servicePackage.getCoverImage())
                .title(servicePackage.getTitle())
                .brand(brand)
                .creator(servicePackage.getCreator())
                .price(servicePackage.getPrice())
                .paymentIntent(paymentIntent)
                .completed(false)
                .build();

        orderRepository.save(order);
        return new PaymentIntentResponse(false, clientSecret);
    }

    public void confirmPayment(String paymentIntent) {
        Order order = orderRepository.findByPaymentIntent(paymentIntent)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));

        order.setCompleted(true);
        orderRepository.save(order);
    }
}

