package com.zingzing.backend.service;

import com.zingzing.backend.entity.DisputeCase;
import com.zingzing.backend.entity.Order;
import com.zingzing.backend.entity.enums.UserRole;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.repository.DisputeCaseRepository;
import com.zingzing.backend.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<DisputeCase> getMyDisputes(UUID userId, UserRole role, int page, int limit) {
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(limit, 1), 50));
        if (role.isAdmin()) {
            return disputeCaseRepository.findAll(pageable);
        }
        return disputeCaseRepository.findByParticipantId(userId, pageable);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public DisputeCase getDispute(UUID disputeId, UUID userId, UserRole role) {
        DisputeCase dispute = disputeCaseRepository.findById(disputeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Dispute not found"));
        if (!role.isAdmin()) {
            boolean isParticipant = dispute.getOrder().getCreator().getId().equals(userId)
                    || dispute.getOrder().getBrand().getId().equals(userId);
            if (!isParticipant) throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return dispute;
    }
}
