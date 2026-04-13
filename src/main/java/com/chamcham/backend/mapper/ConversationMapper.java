package com.chamcham.backend.mapper;

import com.chamcham.backend.dto.conversation.ConversationResponse;
import com.chamcham.backend.entity.Conversation;
import org.springframework.stereotype.Component;

@Component
public class ConversationMapper {

    public ConversationResponse toResponse(Conversation conversation) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getSeller().getId(),
                conversation.getBuyer().getId(),
                conversation.isReadBySeller(),
                conversation.isReadByBuyer(),
                conversation.getLastMessage(),
                conversation.getUpdatedAt()
        );
    }
}

