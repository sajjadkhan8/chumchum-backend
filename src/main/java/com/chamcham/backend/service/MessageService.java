package com.chamcham.backend.service;

import com.chamcham.backend.dto.message.MessageCreateRequest;
import com.chamcham.backend.dto.message.MessageResponse;
import com.chamcham.backend.entity.Conversation;
import com.chamcham.backend.entity.Message;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.entity.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.MessageMapper;
import com.chamcham.backend.repository.ConversationRepository;
import com.chamcham.backend.repository.MessageRepository;
import com.chamcham.backend.repository.UserRepository;
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

    public MessageService(MessageRepository messageRepository, ConversationRepository conversationRepository, UserRepository userRepository, MessageMapper messageMapper) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.messageMapper = messageMapper;
    }

    public MessageResponse createMessage(UUID userId, UserRole role, MessageCreateRequest request) {
        if (role.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin cannot send creator/brand messages");
        }

        Conversation conversation = conversationRepository.findById(request.conversationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conversation not found"));

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        Message message = Message.builder()
                .id(UUID.randomUUID())
                .conversation(conversation)
                .sender(sender)
                .description(request.description())
                .build();

        conversation.setReadByCreator(role.isCreator());
        conversation.setReadByBrand(role.isBrand());
        conversation.setLastMessage(request.description());
        conversationRepository.save(conversation);

        return messageMapper.toResponse(messageRepository.save(message));
    }

    public List<MessageResponse> getMessages(UUID conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(messageMapper::toResponse)
                .toList();
    }
}

