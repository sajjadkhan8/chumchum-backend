package com.zingzing.backend.controller;

import com.zingzing.backend.config.security.AuthenticatedUser;
import com.zingzing.backend.dto.order.CreateOrderRequest;
import com.zingzing.backend.dto.order.DeliverableResponse;
import com.zingzing.backend.dto.order.OrderResponse;
import com.zingzing.backend.dto.order.UpdateOrderStatusRequest;
import com.zingzing.backend.entity.enums.OrderStatus;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.ok(orderService.getOrders(authUser.userId(), authUser.role(), page, limit));
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(request.packageId(), authUser.userId(), authUser.role(),
                        request.amount(), request.barterDetails(), request.message(), request.dealType(), idempotencyKey));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID orderId,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.ok(orderService.getOrder(orderId, authUser.userId(), authUser.role()));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.ok(orderService.updateStatus(orderId,
                OrderStatus.valueOf(request.status().toUpperCase()), authUser.userId(), authUser.role()));
    }

    @PatchMapping("/{orderId}/barter-confirm")
    public ResponseEntity<OrderResponse> confirmBarterReceipt(@PathVariable UUID orderId,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.ok(orderService.confirmBarterReceipt(orderId, authUser.userId(), authUser.role()));
    }

    @PatchMapping("/{orderId}/progress")
    public ResponseEntity<OrderResponse> updateOrderProgress(@PathVariable UUID orderId,
            @RequestBody Map<String, Integer> request,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        Integer progress = request.get("progress");
        if (progress == null) throw new ApiException(HttpStatus.BAD_REQUEST, "progress is required");
        return ResponseEntity.ok(orderService.updateProgress(orderId, progress, authUser.userId()));
    }

    @PostMapping("/{orderId}/deliverables/{deliverableId}/submit")
    public ResponseEntity<DeliverableResponse> submitDeliverable(@PathVariable UUID orderId,
            @PathVariable UUID deliverableId, @RequestBody Map<String, String> body,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.ok(orderService.submitDeliverable(orderId, deliverableId,
                body.get("fileUrl"), body.get("note"), authUser.userId(), authUser.role()));
    }

    @PatchMapping("/{orderId}/deliverables/{deliverableId}/status")
    public ResponseEntity<DeliverableResponse> updateDeliverableStatus(@PathVariable UUID orderId,
            @PathVariable UUID deliverableId, @RequestBody Map<String, String> body,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        String raw = body.get("status");
        if (raw == null) throw new ApiException(HttpStatus.BAD_REQUEST, "status is required");
        return ResponseEntity.ok(orderService.reviewDeliverable(orderId, deliverableId, raw, body.get("comment"),
                authUser.userId(), authUser.role()));
    }
}
