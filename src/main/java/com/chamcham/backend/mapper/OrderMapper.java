package com.chamcham.backend.mapper;

import com.chamcham.backend.dto.order.DeliverableResponse;
import com.chamcham.backend.dto.order.OrderResponse;
import com.chamcham.backend.entity.Deliverable;
import com.chamcham.backend.entity.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        List<DeliverableResponse> deliverables = order.getDeliverables() == null
                ? List.of()
                : order.getDeliverables().stream().map(this::toDeliverableResponse).toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getServicePackage().getId(),
                order.getServicePackage().getTitle(),
                order.getCreator().getId(),
                order.getCreator().getName(),
                order.getBrand().getId(),
                order.getBrand().getDisplayName(),
                order.getDealType() != null ? order.getDealType().name().toLowerCase() : "paid",
                order.getAmount(),
                order.getBarterDetails(),
                order.getMessage(),
                order.getStatus() != null ? order.getStatus().name().toLowerCase() : "pending",
                order.getProgress(),
                order.getDeadlineDate(),
                order.getDeliveryDate(),
                order.getCreatedAt(),
                deliverables
        );
    }

    private DeliverableResponse toDeliverableResponse(Deliverable deliverable) {
        return new DeliverableResponse(
                deliverable.getId(),
                deliverable.getOrder().getId(),
                deliverable.getName(),
                deliverable.getStatus() != null ? deliverable.getStatus().name().toLowerCase() : "pending",
                deliverable.getFileUrl(),
                deliverable.getSubmittedAt(),
                deliverable.getRevisionNote(),
                deliverable.getCreatedAt()
        );
    }
}
