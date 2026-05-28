package com.chamcham.backend.controller;

import com.chamcham.backend.config.security.AuthenticatedUser;
import com.chamcham.backend.dto.order.CreateOrderRequest;
import com.chamcham.backend.dto.order.OrderResponse;
import com.chamcham.backend.dto.order.UpdateOrderStatusRequest;
import com.chamcham.backend.entity.enums.OrderStatus;
import com.chamcham.backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(@AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.ok(orderService.getOrders(authUser.userId(), authUser.role()));
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(
                        request.packageId(),
                        authUser.userId(),
                        authUser.role(),
                        request.amount(),
                        request.barterDetails(),
                        request.message(),
                        request.dealType()
                ));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        return ResponseEntity.ok(orderService.getOrder(orderId, authUser.userId(), authUser.role()));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        OrderStatus newStatus = OrderStatus.valueOf(request.status().toUpperCase());
        return ResponseEntity.ok(orderService.updateStatus(orderId, newStatus, authUser.userId(), authUser.role()));
    }

    @PatchMapping("/{orderId}/progress")
    public ResponseEntity<OrderResponse> updateOrderProgress(
            @PathVariable UUID orderId,
            @RequestBody java.util.Map<String, Integer> request,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        Integer progress = request.get("progress");
        if (progress == null) {
            throw new com.chamcham.backend.exception.ApiException(HttpStatus.BAD_REQUEST, "progress is required");
        }
        return ResponseEntity.ok(orderService.updateProgress(orderId, progress, authUser.userId()));
    }
}

