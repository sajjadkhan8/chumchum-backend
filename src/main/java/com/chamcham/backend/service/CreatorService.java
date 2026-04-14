package com.chamcham.backend.service;

import com.chamcham.backend.dto.creator.CreatorCreateRequest;
import com.chamcham.backend.dto.creator.CreatorResponse;
import com.chamcham.backend.dto.creator.CreatorUpdateRequest;
import com.chamcham.backend.entity.Creator;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.CreatorMapper;
import com.chamcham.backend.repository.CreatorRepository;
import com.chamcham.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class CreatorService {

    private final CreatorRepository creatorRepository;
    private final UserRepository userRepository;
    private final CreatorMapper creatorMapper;

    public CreatorService(CreatorRepository creatorRepository, UserRepository userRepository, CreatorMapper creatorMapper) {
        this.creatorRepository = creatorRepository;
        this.userRepository = userRepository;
        this.creatorMapper = creatorMapper;
    }

    @Transactional
    public CreatorResponse create(CreatorCreateRequest request) {
        if (request.userId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "userId is required");
        }

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (!user.getRole().isCreator()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "User role must be CREATOR");
        }

        if (creatorRepository.findById(user.getId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Creator profile already exists for this user");
        }

        int insertedRows = creatorRepository.insertProfile(
                user.getId(),
                request.bio(),
                request.category(),
                request.tiktokUrl(),
                request.instagramUrl(),
                request.youtubeUrl(),
                0,
                0,
                null,
                BigDecimal.ZERO,
                0
        );

        if (insertedRows != 1) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create creator profile");
        }

        Creator created = creatorRepository.findById(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Creator profile created but could not be loaded"));
        return creatorMapper.toResponse(created);
    }

    @Transactional
    public List<CreatorResponse> getAll() {
        return creatorRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(creatorMapper::toResponse)
                .toList();
    }

    @Transactional
    public CreatorResponse getById(UUID creatorId) {
        return creatorMapper.toResponse(findCreator(creatorId));
    }

    @Transactional
    public CreatorResponse getByUserId(UUID actorUserId, UserRole actorRole, UUID userId) {
        validateOwnerOrAdmin(actorUserId, actorRole, userId);
        Creator creator = creatorRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator profile not found"));
        return creatorMapper.toResponse(creator);
    }

    @Transactional
    public CreatorResponse update(UUID actorUserId, UserRole actorRole, UUID creatorId, CreatorUpdateRequest request) {
        Creator creator = findCreator(creatorId);
        validateOwnerOrAdmin(actorUserId, actorRole, creator.getId());
        validateMetricsAccess(actorRole, request);

        if (request.bio() != null) {
            creator.setBio(request.bio());
        }
        if (request.category() != null) {
            creator.setCategory(request.category());
        }
        if (request.tiktokUrl() != null) {
            creator.setTiktokUrl(request.tiktokUrl());
        }
        if (request.instagramUrl() != null) {
            creator.setInstagramUrl(request.instagramUrl());
        }
        if (request.youtubeUrl() != null) {
            creator.setYoutubeUrl(request.youtubeUrl());
        }
        if (request.followers() != null) {
            creator.setFollowers(request.followers());
        }
        if (request.avgViews() != null) {
            creator.setAvgViews(request.avgViews());
        }
        if (request.engagementRate() != null) {
            creator.setEngagementRate(request.engagementRate());
        }
        if (request.rating() != null) {
            creator.setRating(request.rating());
        }
        if (request.totalReviews() != null) {
            creator.setTotalReviews(request.totalReviews());
        }

        return creatorMapper.toResponse(creatorRepository.save(creator));
    }

    private Creator findCreator(UUID creatorId) {
        return creatorRepository.findById(creatorId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator profile not found"));
    }

    private void validateOwnerOrAdmin(UUID actorUserId, UserRole actorRole, UUID resourceUserId) {
        if (!actorRole.isAdmin() && !actorUserId.equals(resourceUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only manage your own creator profile");
        }
    }

    private void validateMetricsAccess(UserRole actorRole, CreatorUpdateRequest request) {
        boolean metricsProvided = request.followers() != null
                || request.avgViews() != null
                || request.engagementRate() != null
                || request.rating() != null
                || request.totalReviews() != null;

        if (metricsProvided && !actorRole.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only admin can update creator metrics");
        }
    }
}
