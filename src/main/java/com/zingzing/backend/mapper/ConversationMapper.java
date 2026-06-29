package com.zingzing.backend.mapper;

import com.zingzing.backend.dto.conversation.ConversationResponse;
import com.zingzing.backend.entity.Brand;
import com.zingzing.backend.entity.Conversation;
import com.zingzing.backend.entity.Creator;
import com.zingzing.backend.entity.enums.UserRole;
import com.zingzing.backend.repository.OrderRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;

@Component
public class ConversationMapper {
    private static final Duration ONLINE_WINDOW = Duration.ofSeconds(90);

    private final OrderRepository orderRepository;

    public ConversationMapper(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

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
        ContextSummary context = contextSummary(conversation);
        return new ConversationResponse(
                conversation.getId(),
                creator.getId(),
                brand.getId(),
                conversation.getContextType().name().toLowerCase(),
                conversation.getContextId(),
                context.label(),
                context.title(),
                context.status(),
                context.amount(),
                context.deadlineDate(),
                conversation.getUnreadCountCreator(),
                conversation.getUnreadCountBrand(),
                hideLastMessage ? null : conversation.getLastMessage(),
                conversation.getUpdatedAt(),
                creator.getName(),
                creator.getAvatarUrl(),
                isOnline(creator.getLastSeenAt()),
                creator.getLastSeenAt(),
                brand.getDisplayName(),
                brand.getLogoUrl(),
                isOnline(brand.getLastSeenAt()),
                brand.getLastSeenAt(),
                blockedByMe,
                blockedByThem
        );
    }

    private boolean isOnline(Instant lastSeenAt) {
        return lastSeenAt != null && lastSeenAt.isAfter(Instant.now().minus(ONLINE_WINDOW));
    }

    private ContextSummary contextSummary(Conversation conversation) {
        if (conversation.getContextType() == Conversation.ContextType.ORDER && conversation.getContextId() != null) {
            return orderRepository.findByIdWithDetails(conversation.getContextId())
                    .map(order -> new ContextSummary(
                            order.getOrderNumber() == null ? "Order" : "Order " + order.getOrderNumber(),
                            order.getServicePackage().getTitle(),
                            order.getStatus() == null ? null : order.getStatus().name().toLowerCase(),
                            order.getAmount(),
                            order.getDeadlineDate()
                    ))
                    .orElse(new ContextSummary("Order", "Order conversation", null, null, null));
        }
        return new ContextSummary("General", null, null, null, null);
    }

    private record ContextSummary(
            String label,
            String title,
            String status,
            Integer amount,
            OffsetDateTime deadlineDate
    ) {
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
