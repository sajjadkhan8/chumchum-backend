package com.chamcham.backend.mapper;

import com.chamcham.backend.dto.order.OrderResponse;
import com.chamcham.backend.entity.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getGig().getId(),
                order.getImage(),
                order.getTitle(),
                order.getPrice(),
                order.getSeller().getId(),
                order.getBrand().getId(),
                order.isCompleted(),
                order.getPaymentIntent(),
                order.getCreatedAt()
        );
    }
}

