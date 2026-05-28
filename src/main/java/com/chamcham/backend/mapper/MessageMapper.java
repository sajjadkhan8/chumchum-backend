package com.chamcham.backend.mapper;

import com.chamcham.backend.dto.message.MessageResponse;
import com.chamcham.backend.entity.Message;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    public MessageResponse toResponse(Message message) {
        // prefer content field, fallback to legacy description
        String content = message.getContent() != null ? message.getContent() : message.getDescription();
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getSender().getId(),
                message.getSenderType(),
                message.getType() != null ? message.getType().name().toLowerCase() : "text",
                content,
                message.isRead(),
                message.getAttachmentUrl(),
                message.getOfferDealType(),
                message.getOfferAmount(),
                message.getOfferBarterDetails(),
                message.getOfferBarterCategory(),
                message.getOfferStatus(),
                message.getCreatedAt()
        );
    }
}
