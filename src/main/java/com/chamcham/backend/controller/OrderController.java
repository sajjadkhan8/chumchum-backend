package com.chamcham.backend.controller;

import com.chamcham.backend.config.security.AuthenticatedUser;
import com.chamcham.backend.dto.order.CreateOrderRequest;
import com.chamcham.backend.dto.order.DeliverableResponse;
import com.chamcham.backend.dto.order.OrderResponse;
import com.chamcham.backend.dto.order.UpdateOrderStatusRequest;
import com.chamcham.backend.entity.Deliverable;
import com.chamcham.backend.entity.Order;
import com.chamcham.backend.entity.enums.OrderStatus;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.repository.DeliverableRepository;
import com.chamcham.backend.repository.OrderRepository;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final DeliverableRepository deliverableRepository;

    public OrderController(OrderService orderService, OrderRepository orderRepository,
                           DeliverableRepository deliverableRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.deliverableRepository = deliverableRepository;
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(@AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.ok(orderService.getOrders(authUser.userId(), authUser.role()));
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(request.packageId(), authUser.userId(), authUser.role(),
                        request.amount(), request.barterDetails(), request.message(), request.dealType()));
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
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getCreator().getId().equals(authUser.userId()) && !authUser.role().isAdmin())
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        Deliverable d = deliverableRepository.findById(deliverableId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Deliverable not found"));
        if (!d.getOrder().getId().equals(orderId))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Deliverable does not belong to this order");
        String fileUrl = body.get("fileUrl");
        String note = body.get("note");
        if (fileUrl != null) d.setFileUrl(fileUrl);
        if (note != null && !note.isBlank()) {
            String ex = d.getName();
            d.setName(ex == null ? note : ex + " - " + note);
        }
        d.setStatus(Deliverable.DeliverableStatus.REVIEW);
        d.setSubmittedAt(OffsetDateTime.now());
        return ResponseEntity.ok(toDeliverableResponse(deliverableRepository.save(d)));
    }

    @PatchMapping("/{orderId}/deliverables/{deliverableId}/status")
    public ResponseEntity<DeliverableResponse> updateDeliverableStatus(@PathVariable UUID orderId,
            @PathVariable UUID deliverableId, @RequestBody Map<String, String> body,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getCreator().getId().equals(authUser.userId())
                && !order.getBrand().getId().equals(authUser.userId())
                && !authUser.role().isAdmin())
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        Deliverable d = deliverableRepository.findById(deliverableId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Deliverable not found"));
        if (!d.getOrder().getId().equals(orderId))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Deliverable does not belong to this order");
        String raw = body.get("status");
        if (raw == null) throw new ApiException(HttpStatus.BAD_REQUEST, "status is required");
        try {
            d.setStatus(Deliverable.DeliverableStatus.valueOf(raw.trim().toUpperCase()));
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid status: " + raw);
        }
        return ResponseEntity.ok(toDeliverableResponse(deliverableRepository.save(d)));
    }

    private DeliverableResponse toDeliverableResponse(Deliverable d) {
        return new DeliverableResponse(d.getId(), d.getOrder().getId(), d.getName(),
                d.getStatus().name().toLowerCase(), d.getFileUrl(), d.getSubmittedAt(), d.getCreatedAt());
    }
}
