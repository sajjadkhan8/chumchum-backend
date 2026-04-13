package com.chamcham.backend.service;

import com.chamcham.backend.dto.conversation.ConversationCreateRequest;
import com.chamcham.backend.dto.conversation.ConversationResponse;
import com.chamcham.backend.entity.Conversation;
import com.chamcham.backend.entity.User;
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

    public ConversationResponse createConversation(UUID userId, boolean isSeller, ConversationCreateRequest request) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        UUID sellerId = isSeller ? userId : request.to();
        UUID buyerId = isSeller ? request.to() : userId;

        if (sellerId.equals(buyerId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Seller and buyer cannot be the same user");
        }

        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Seller not found"));
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Buyer not found"));

        Conversation conversation = conversationRepository.findBySellerIdAndBuyerId(sellerId, buyerId)
                .orElse(Conversation.builder()
                        .id(UUID.randomUUID())
                        .seller(seller)
                        .buyer(buyer)
                        .readBySeller(isSeller)
                        .readByBuyer(!isSeller)
                        .build());

        if (!conversation.getSeller().getId().equals(currentUser.getId())
                && !conversation.getBuyer().getId().equals(currentUser.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cannot create or access this conversation");
        }

        return conversationMapper.toResponse(conversationRepository.save(conversation));
    }

    public List<ConversationResponse> getConversations(UUID userId, boolean isSeller) {
        List<Conversation> conversations = isSeller
                ? conversationRepository.findBySellerIdOrderByUpdatedAtDesc(userId)
                : conversationRepository.findByBuyerIdOrderByUpdatedAtDesc(userId);

        return conversations.stream().map(conversationMapper::toResponse).toList();
    }

    public ConversationResponse getSingleConversation(UUID sellerId, UUID buyerId) {
        Conversation conversation = conversationRepository.findBySellerIdAndBuyerId(sellerId, buyerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No such conversation found!"));
        return conversationMapper.toResponse(conversation);
    }

    public ConversationResponse markAsRead(UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conversation not found"));

        conversation.setReadBySeller(true);
        conversation.setReadByBuyer(true);

        return conversationMapper.toResponse(conversationRepository.save(conversation));
    }
}

