package com.zingzing.backend.mapper;

import com.zingzing.backend.dto.conversation.ConversationResponse;
import com.zingzing.backend.entity.Brand;
import com.zingzing.backend.entity.Conversation;
import com.zingzing.backend.entity.Creator;
import org.springframework.stereotype.Component;

@Component
public class ConversationMapper {

    public ConversationResponse toResponse(Conversation conversation) {
        Creator creator = conversation.getCreator();
        Brand brand = conversation.getBrand();
        return new ConversationResponse(
                conversation.getId(),
                creator.getId(),
                brand.getId(),
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
