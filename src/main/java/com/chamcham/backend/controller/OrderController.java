package com.chamcham.backend.controller;

import com.chamcham.backend.config.security.AuthenticatedUser;
import com.chamcham.backend.dto.order.OrderResponse;
import com.chamcham.backend.dto.order.PaymentConfirmRequest;
import com.chamcham.backend.dto.order.PaymentIntentResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(@AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.ok(orderService.getOrders(authUser.userId()));
    }

    @PostMapping("/create-payment-intent/{packageId}")
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(
            @PathVariable UUID packageId,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        return ResponseEntity.ok(orderService.createPaymentIntent(packageId, authUser.userId(), authUser.role()));
    }

    @PatchMapping
    public ResponseEntity<Map<String, Object>> confirmOrder(@Valid @RequestBody PaymentConfirmRequest request) {
        orderService.confirmPayment(request.paymentIntent());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("error", false, "message", "Order has been confirmed!"));
    }
}

