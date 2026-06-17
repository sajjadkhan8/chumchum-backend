package com.chamcham.backend.service;

import com.chamcham.backend.entity.DisputeCase;
import com.chamcham.backend.entity.Order;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.repository.DisputeCaseRepository;
import com.chamcham.backend.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DisputeService {

    private final DisputeCaseRepository disputeCaseRepository;
    private final OrderRepository orderRepository;

    public DisputeService(DisputeCaseRepository disputeCaseRepository,
                          OrderRepository orderRepository) {
        this.disputeCaseRepository = disputeCaseRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public DisputeCase openDispute(UUID userId, UserRole role, UUID orderId, String title, String description) {
        if (role.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admins use the admin panel to create disputes");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));

        boolean isParticipant = order.getCreator().getId().equals(userId)
                || order.getBrand().getId().equals(userId);
        if (!isParticipant) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not a participant in this order");
        }

        if (order.getStatus().name().equals("CANCELLED")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot open a dispute on a cancelled order");
        }

        if (disputeCaseRepository.existsByOrderId(orderId)) {
            throw new ApiException(HttpStatus.CONFLICT, "A dispute already exists for this order");
        }

        if (title == null || title.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "title is required");
        }
        if (description == null || description.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "description is required");
        }

        DisputeCase dispute = disputeCaseRepository.save(DisputeCase.builder()
                .order(order)
                .title(title.trim())
                .description(description.trim())
                .priority("normal")
                .build());

        // Force-load lazy associations so the controller can serialize the response
        dispute.getOrder().getCreator().getName();
        dispute.getOrder().getBrand().getName();
        dispute.getOrder().getServicePackage().getTitle();

        return dispute;
    }
}
