package com.chamcham.backend.service;

import com.chamcham.backend.dto.message.MessageCreateRequest;
import com.chamcham.backend.dto.message.MessageResponse;
import com.chamcham.backend.entity.Conversation;
import com.chamcham.backend.entity.Message;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.MessageMapper;
import com.chamcham.backend.repository.ConversationRepository;
import com.chamcham.backend.repository.MessageRepository;
import com.chamcham.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final MessageMapper messageMapper;

    public MessageService(MessageRepository messageRepository,
                          ConversationRepository conversationRepository,
                          UserRepository userRepository,
                          MessageMapper messageMapper) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.messageMapper = messageMapper;
    }

    @Transactional
    public MessageResponse sendTextMessage(UUID userId, UserRole role, UUID conversationId,
                                           MessageCreateRequest request) {
        if (role.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin cannot send messages");
        }
        Conversation conversation = findConversation(conversationId);
        validateParticipant(userId, conversation);
        User sender = findUser(userId);

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .senderType(role.name().toLowerCase())
                .content(request.content())
                .type(Message.MessageType.TEXT)
                .isRead(false)
                .build();

        return save(message, conversation, role, request.content());
    }

    @Transactional
    public MessageResponse sendOfferMessage(UUID userId, UserRole role, UUID conversationId,
                                            MessageCreateRequest request) {
        if (role.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin cannot send offers");
        }
        Conversation conversation = findConversation(conversationId);
        validateParticipant(userId, conversation);
        User sender = findUser(userId);

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .senderType(role.name().toLowerCase())
                .content(request.content())
                .type(Message.MessageType.OFFER)
                .offerDealType(request.offerDealType())
                .offerAmount(request.offerAmount())
                .offerBarterDetails(request.offerBarterDetails())
                .offerBarterCategory(request.offerBarterCategory())
                .offerMessage(request.content())
                .offerStatus("pending")
                .isRead(false)
                .build();

        return save(message, conversation, role, "[Offer]");
    }

    public List<MessageResponse> getMessages(UUID conversationId, UUID userId) {
        Conversation conversation = findConversation(conversationId);
        validateParticipant(userId, conversation);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream().map(messageMapper::toResponse).toList();
    }

    @Transactional
    public void markRead(UUID conversationId, UUID userId, UserRole role) {
        Conversation conversation = findConversation(conversationId);
        validateParticipant(userId, conversation);
        if (role.isCreator()) {
            conversation.setUnreadCountCreator(0);
            conversation.setReadByCreator(true);
        } else {
            conversation.setUnreadCountBrand(0);
            conversation.setReadByBrand(true);
        }
        conversationRepository.save(conversation);
    }

    // ---- helpers ----

    private MessageResponse save(Message message, Conversation conversation, UserRole senderRole, String preview) {
        Message saved = messageRepository.save(message);
        // increment unread for the OTHER party
        if (senderRole.isCreator()) {
            conversation.setUnreadCountBrand(conversation.getUnreadCountBrand() + 1);
            conversation.setReadByCreator(true);
            conversation.setReadByBrand(false);
        } else {
            conversation.setUnreadCountCreator(conversation.getUnreadCountCreator() + 1);
            conversation.setReadByBrand(true);
            conversation.setReadByCreator(false);
        }
        conversation.setLastMessage(preview != null && preview.length() > 200
                ? preview.substring(0, 200) : preview);
        conversation.setLastMessageId(saved.getId());
        conversationRepository.save(conversation);
        return messageMapper.toResponse(saved);
    }

    private void validateParticipant(UUID userId, Conversation conv) {
        if (!conv.getCreator().getId().equals(userId) && !conv.getBrand().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not a participant in this conversation");
        }
    }

    private Conversation findConversation(UUID id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conversation not found"));
    }

    private User findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
