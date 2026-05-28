package com.chamcham.backend.mapper;

import com.chamcham.backend.dto.order.OrderResponse;
import com.chamcham.backend.entity.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                        order.getOrderNumber(),
                order.getServicePackage().getId(),
                order.getCreator().getId(),
                order.getBrand().getId(),
                        order.getDealType() != null ? order.getDealType().name().toLowerCase() : "paid",
                        order.getAmount(),
                        order.getBarterDetails(),
                        order.getStatus() != null ? order.getStatus().name().toLowerCase() : "pending",
                        order.getProgress(),
                        order.getDeadlineDate(),
                        order.getDeliveryDate(),
                        order.getImage(),
                        order.getTitle(),
                order.getCreatedAt()
        );
    }
}

