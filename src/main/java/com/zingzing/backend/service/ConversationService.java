package com.zingzing.backend.service;

import com.zingzing.backend.dto.conversation.ConversationCreateRequest;
import com.zingzing.backend.dto.conversation.ConversationResponse;
import com.zingzing.backend.entity.Brand;
import com.zingzing.backend.entity.Conversation;
import com.zingzing.backend.entity.Creator;
import com.zingzing.backend.entity.enums.UserRole;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.mapper.ConversationMapper;
import com.zingzing.backend.repository.BrandRepository;
import com.zingzing.backend.repository.ConversationRepository;
import com.zingzing.backend.repository.CreatorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
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

    @Transactional
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
                        .build());

        if (!conversation.getCreator().getId().equals(userId)
                && !conversation.getBrand().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cannot create or access this conversation");
        }

        return conversationMapper.toResponse(conversationRepository.save(conversation), role);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getConversations(UUID userId, UserRole role, int page, int limit) {
        if (role.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin does not have creator/brand conversations");
        }

        int safePage = Math.max(0, page);
        int safeLimit = Math.min(Math.max(1, limit), 100);
        PageRequest pageable = PageRequest.of(safePage, safeLimit);

        Page<Conversation> conversationPage = role.isCreator()
                ? conversationRepository.findByCreatorIdOrderByUpdatedAtDesc(userId, pageable)
                : conversationRepository.findByBrandIdOrderByUpdatedAtDesc(userId, pageable);

        List<ConversationResponse> responseList = conversationPage.getContent()
                .stream().map(conversation -> conversationMapper.toResponse(conversation, role)).toList();

        return Map.of(
                "items", responseList,
                "total", conversationPage.getTotalElements(),
                "page", safePage,
                "limit", safeLimit
        );
    }

    @Transactional(readOnly = true)
    public ConversationResponse getSingleConversation(UUID creatorId, UUID brandId, UUID userId, UserRole role) {
        if (role.isAdmin() || (!creatorId.equals(userId) && !brandId.equals(userId))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cannot access this conversation");
        }
        Conversation conversation = conversationRepository.findByCreatorIdAndBrandId(creatorId, brandId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No such conversation found!"));
        return conversationMapper.toResponse(conversation, role);
    }
}
