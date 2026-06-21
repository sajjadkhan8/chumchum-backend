package com.zingzing.backend.mapper;

import com.zingzing.backend.dto.conversation.ConversationResponse;
import com.zingzing.backend.entity.Brand;
import com.zingzing.backend.entity.Conversation;
import com.zingzing.backend.entity.Creator;
import com.zingzing.backend.entity.enums.UserRole;
import org.springframework.stereotype.Component;

@Component
public class ConversationMapper {

    public ConversationResponse toResponse(Conversation conversation) {
        return toResponse(conversation, null);
    }

    public ConversationResponse toResponse(Conversation conversation, UserRole viewerRole) {
        Creator creator = conversation.getCreator();
        Brand brand = conversation.getBrand();
        boolean viewerIsCreator = viewerRole != null && viewerRole.isCreator();
        boolean viewerIsBrand = viewerRole != null && viewerRole.isBrand();
        boolean blockedByMe = (viewerIsCreator && conversation.getBlockedAtCreator() != null)
                || (viewerIsBrand && conversation.getBlockedAtBrand() != null);
        boolean blockedByThem = (viewerIsCreator && conversation.getBlockedAtBrand() != null)
                || (viewerIsBrand && conversation.getBlockedAtCreator() != null);
        boolean hideLastMessage = (viewerIsCreator && isLastMessageClearedForCreator(conversation))
                || (viewerIsBrand && isLastMessageClearedForBrand(conversation));
        return new ConversationResponse(
                conversation.getId(),
                creator.getId(),
                brand.getId(),
                conversation.getUnreadCountCreator(),
                conversation.getUnreadCountBrand(),
                hideLastMessage ? null : conversation.getLastMessage(),
                conversation.getUpdatedAt(),
                creator.getName(),
                creator.getAvatarUrl(),
                brand.getDisplayName(),
                brand.getLogoUrl(),
                blockedByMe,
                blockedByThem
        );
    }

    private boolean isLastMessageClearedForCreator(Conversation conversation) {
        return conversation.getClearedAtCreator() != null
                && (conversation.getLastMessageAt() == null
                || !conversation.getLastMessageAt().isAfter(conversation.getClearedAtCreator()));
    }

    private boolean isLastMessageClearedForBrand(Conversation conversation) {
        return conversation.getClearedAtBrand() != null
                && (conversation.getLastMessageAt() == null
                || !conversation.getLastMessageAt().isAfter(conversation.getClearedAtBrand()));
    }
}
