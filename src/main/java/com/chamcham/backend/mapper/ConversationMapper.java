package com.chamcham.backend.mapper;

import com.chamcham.backend.dto.conversation.ConversationResponse;
import com.chamcham.backend.entity.Conversation;
import org.springframework.stereotype.Component;

@Component
public class ConversationMapper {

    public ConversationResponse toResponse(Conversation conversation) {
        com.chamcham.backend.entity.Creator creator = conversation.getCreator();
        com.chamcham.backend.entity.Brand brand = conversation.getBrand();
        return new ConversationResponse(
                conversation.getId(),
                creator.getId(),
                brand.getId(),
                conversation.isReadByCreator(),
                conversation.isReadByBrand(),
                conversation.getUnreadCountCreator(),
                conversation.getUnreadCountBrand(),
                conversation.getLastMessage(),
                conversation.getUpdatedAt(),
                creator.getName(),
                creator.getAvatarUrl(),
                brand.getDisplayName(),
                brand.getLogoUrl()
        );
    }
}
