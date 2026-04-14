package com.chamcham.backend.service;

import com.chamcham.backend.dto.conversation.ConversationCreateRequest;
import com.chamcham.backend.dto.conversation.ConversationResponse;
import com.chamcham.backend.entity.Brand;
import com.chamcham.backend.entity.Conversation;
import com.chamcham.backend.entity.Creator;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.ConversationMapper;
import com.chamcham.backend.repository.BrandRepository;
import com.chamcham.backend.repository.ConversationRepository;
import com.chamcham.backend.repository.CreatorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final CreatorRepository creatorRepository;
    private final BrandRepository brandRepository;
    private final ConversationMapper conversationMapper;

    public ConversationService(
            ConversationRepository conversationRepository,
            CreatorRepository creatorRepository,
            BrandRepository brandRepository,
            ConversationMapper conversationMapper
    ) {
        this.conversationRepository = conversationRepository;
        this.creatorRepository = creatorRepository;
        this.brandRepository = brandRepository;
        this.conversationMapper = conversationMapper;
    }

    public ConversationResponse createConversation(UUID userId, UserRole role, ConversationCreateRequest request) {
        if (role.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin cannot start marketplace conversations");
        }

        boolean isCreator = role.isCreator();
        UUID creatorId = isCreator ? userId : request.to();
        UUID brandId = isCreator ? request.to() : userId;

        if (creatorId.equals(brandId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Creator and brand cannot be the same user");
        }

        Creator creator = creatorRepository.findById(creatorId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator not found"));
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Brand not found"));

        Conversation conversation = conversationRepository.findByCreatorIdAndBrandId(creatorId, brandId)
                .orElse(Conversation.builder()
                        .id(UUID.randomUUID())
                        .creator(creator)
                        .brand(brand)
                        .readByCreator(isCreator)
                        .readByBrand(!isCreator)
                        .build());

        if (!conversation.getCreator().getId().equals(userId)
                && !conversation.getBrand().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cannot create or access this conversation");
        }

        return conversationMapper.toResponse(conversationRepository.save(conversation));
    }

    public List<ConversationResponse> getConversations(UUID userId, UserRole role) {
        if (role.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin does not have creator/brand conversations");
        }

        List<Conversation> conversations = role.isCreator()
                ? conversationRepository.findByCreatorIdOrderByUpdatedAtDesc(userId)
                : conversationRepository.findByBrandIdOrderByUpdatedAtDesc(userId);

        return conversations.stream().map(conversationMapper::toResponse).toList();
    }

    public ConversationResponse getSingleConversation(UUID creatorId, UUID brandId) {
        Conversation conversation = conversationRepository.findByCreatorIdAndBrandId(creatorId, brandId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No such conversation found!"));
        return conversationMapper.toResponse(conversation);
    }

    public ConversationResponse markAsRead(UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conversation not found"));

        conversation.setReadByCreator(true);
        conversation.setReadByBrand(true);

        return conversationMapper.toResponse(conversationRepository.save(conversation));
    }
}
