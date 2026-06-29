package com.zingzing.backend.mapper;

import com.zingzing.backend.dto.order.DeliverableResponse;
import com.zingzing.backend.dto.order.OrderResponse;
import com.zingzing.backend.entity.Deliverable;
import com.zingzing.backend.entity.Order;
import com.zingzing.backend.entity.Review;
import com.zingzing.backend.repository.ConversationRepository;
import com.zingzing.backend.repository.DeliverableRepository;
import com.zingzing.backend.repository.ReviewRepository;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class OrderMapper {

    private final ConversationRepository conversationRepository;
    private final ReviewRepository reviewRepository;
    private final DeliverableRepository deliverableRepository;

    public OrderMapper(ConversationRepository conversationRepository,
                       ReviewRepository reviewRepository,
                       DeliverableRepository deliverableRepository) {
        this.conversationRepository = conversationRepository;
        this.reviewRepository = reviewRepository;
        this.deliverableRepository = deliverableRepository;
    }

    public OrderResponse toResponse(Order order) {
        List<Deliverable> deliverableEntities;
        if (order.getDeliverables() == null) {
            deliverableEntities = List.of();
        } else if (Hibernate.isInitialized(order.getDeliverables())) {
            deliverableEntities = order.getDeliverables();
        } else {
            // Detached order: reload deliverables by order id instead of triggering lazy init.
            deliverableEntities = deliverableRepository.findByOrderId(order.getId());
        }
        List<DeliverableResponse> deliverables = deliverableEntities.stream()
                .map(this::toDeliverableResponse)
                .toList();

        UUID conversationId = conversationRepository
                .findByContextTypeAndContextId(com.zingzing.backend.entity.Conversation.ContextType.ORDER, order.getId())
                .map(c -> c.getId())
                .orElse(null);

        boolean hasReviewedByBrand = reviewRepository
                .findByOrderIdAndReviewerType(order.getId(), Review.ReviewerType.BRAND)
                .isPresent();

        boolean hasReviewedByCreator = reviewRepository
                .findByOrderIdAndReviewerType(order.getId(), Review.ReviewerType.CREATOR)
                .isPresent();

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
                order.getCancellationNote(),
                order.getStatus() != null ? order.getStatus().name().toLowerCase() : "pending",
                order.getProgress(),
                order.getDeadlineDate(),
                order.getDeliveryDate(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                deliverables,
                order.isBarterProductReceived(),
                conversationId,
                hasReviewedByBrand,
                hasReviewedByCreator
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
