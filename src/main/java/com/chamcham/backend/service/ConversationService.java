package com.chamcham.backend.service;

import com.chamcham.backend.dto.conversation.ConversationCreateRequest;
import com.chamcham.backend.dto.conversation.ConversationResponse;
import com.chamcham.backend.entity.Conversation;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.entity.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.ConversationMapper;
import com.chamcham.backend.repository.ConversationRepository;
import com.chamcham.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final ConversationMapper conversationMapper;

    public ConversationService(ConversationRepository conversationRepository, UserRepository userRepository, ConversationMapper conversationMapper) {
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.conversationMapper = conversationMapper;
    }

    public ConversationResponse createConversation(UUID userId, UserRole role, ConversationCreateRequest request) {
        if (role.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin cannot start marketplace conversations");
        }

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        boolean isCreator = role.isCreator();
        UUID sellerId = isCreator ? userId : request.to();
        UUID brandId = isCreator ? request.to() : userId;

        if (sellerId.equals(brandId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Creator and brand cannot be the same user");
        }

        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator not found"));
        User brand = userRepository.findById(brandId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Brand not found"));

        Conversation conversation = conversationRepository.findBySellerIdAndBrandId(sellerId, brandId)
                .orElse(Conversation.builder()
                        .id(UUID.randomUUID())
                        .seller(seller)
                        .brand(brand)
                        .readBySeller(isCreator)
                        .readByBrand(!isCreator)
                        .build());

        if (!conversation.getSeller().getId().equals(currentUser.getId())
                && !conversation.getBrand().getId().equals(currentUser.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cannot create or access this conversation");
        }

        return conversationMapper.toResponse(conversationRepository.save(conversation));
    }

    public List<ConversationResponse> getConversations(UUID userId, UserRole role) {
        if (role.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin does not have creator/brand conversations");
        }

        List<Conversation> conversations = role.isCreator()
                ? conversationRepository.findBySellerIdOrderByUpdatedAtDesc(userId)
                : conversationRepository.findByBrandIdOrderByUpdatedAtDesc(userId);

        return conversations.stream().map(conversationMapper::toResponse).toList();
    }

    public ConversationResponse getSingleConversation(UUID sellerId, UUID brandId) {
        Conversation conversation = conversationRepository.findBySellerIdAndBrandId(sellerId, brandId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No such conversation found!"));
        return conversationMapper.toResponse(conversation);
    }

    public ConversationResponse markAsRead(UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conversation not found"));

        conversation.setReadBySeller(true);
        conversation.setReadByBrand(true);

        return conversationMapper.toResponse(conversationRepository.save(conversation));
    }
}

