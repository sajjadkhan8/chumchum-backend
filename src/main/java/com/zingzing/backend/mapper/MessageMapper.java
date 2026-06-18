package com.zingzing.backend.mapper;

import com.zingzing.backend.dto.message.MessageResponse;
import com.zingzing.backend.entity.Message;
import com.zingzing.backend.entity.enums.OfferStatus;
import com.zingzing.backend.repository.OrderRepository;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    private final OrderRepository orderRepository;

    public MessageMapper(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public MessageResponse toResponse(Message message) {
        // prefer content field, fallback to legacy description
        String content = message.getContent() != null ? message.getContent() : message.getDescription();

        String offerDealType = message.getOfferDealType();
        Integer offerAmount = message.getOfferAmount();
        String offerBarterDetails = message.getOfferBarterDetails();
        String offerBarterCategory = message.getOfferBarterCategory();
        String offerStatus = message.getOfferStatus();
        java.util.UUID offerId = null;
        java.util.UUID offerOrderId = null;
        Integer offerEstimatedBarterValue = message.getOfferEstimatedBarterValue();
        String offerCreatorExpectation = message.getOfferCreatorExpectation();

        if (message.getQuickDealOffer() != null) {
            offerId = message.getQuickDealOffer().getId();
            offerDealType = message.getQuickDealOffer().getDealType() != null
                    ? message.getQuickDealOffer().getDealType().name().toLowerCase()
                    : offerDealType;
            offerAmount = message.getQuickDealOffer().getAmount();
            offerBarterDetails = message.getQuickDealOffer().getBarterDetails();
            offerBarterCategory = message.getQuickDealOffer().getBarterCategory();
            offerEstimatedBarterValue = message.getQuickDealOffer().getEstimatedBarterValue();
            offerCreatorExpectation = message.getQuickDealOffer().getCreatorExpectation();
            offerStatus = message.getQuickDealOffer().getStatus() != null
                    ? message.getQuickDealOffer().getStatus().name().toLowerCase()
                    : offerStatus;
            if (message.getQuickDealOffer().getStatus() == OfferStatus.ACCEPTED) {
                offerOrderId = orderRepository.findFirstByServicePackageName("quick-deal-" + offerId)
                        .map(order -> order.getId())
                        .orElse(null);
            }
        }

        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getSender().getId(),
                message.getSenderType(),
                message.getType() != null ? message.getType().name().toLowerCase() : "text",
                content,
                message.isRead(),
                message.getAttachmentUrl(),
                offerDealType,
                offerAmount,
                offerBarterDetails,
                offerBarterCategory,
                offerStatus,
                offerId,
                offerEstimatedBarterValue,
                offerCreatorExpectation,
                offerOrderId,
                message.getCreatedAt()
        );
    }
}
