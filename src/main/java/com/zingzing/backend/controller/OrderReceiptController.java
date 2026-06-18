package com.zingzing.backend.controller;

import com.zingzing.backend.config.security.AuthenticatedUser;
import com.zingzing.backend.entity.Order;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.repository.OrderRepository;
import com.zingzing.backend.service.ReceiptPdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@PreAuthorize("isAuthenticated()")
public class OrderReceiptController {

    private final OrderRepository orderRepository;
    private final ReceiptPdfService receiptPdfService;

    public OrderReceiptController(OrderRepository orderRepository, ReceiptPdfService receiptPdfService) {
        this.orderRepository = orderRepository;
        this.receiptPdfService = receiptPdfService;
    }

    @GetMapping("/{id}/receipt")
    public ResponseEntity<byte[]> getReceipt(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authUser) {

        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));

        boolean isParticipant = order.getCreator().getId().equals(authUser.userId())
                || order.getBrand().getId().equals(authUser.userId())
                || authUser.role().isAdmin();
        if (!isParticipant) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (order.getStatus().name().equals("PENDING") || order.getStatus().name().equals("CANCELLED")) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Receipt is only available for accepted or completed orders");
        }

        byte[] pdf = receiptPdfService.generate(order);
        String filename = "receipt-" + order.getOrderNumber() + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }
}
