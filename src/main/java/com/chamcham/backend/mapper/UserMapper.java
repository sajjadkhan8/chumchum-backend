package com.chamcham.backend.mapper;

import com.chamcham.backend.dto.user.BrandProfilePayload;
import com.chamcham.backend.dto.user.CreatorProfilePayload;
import com.chamcham.backend.dto.user.UserResponse;
import com.chamcham.backend.entity.Brand;
import com.chamcham.backend.entity.Creator;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.repository.BrandRepository;
import com.chamcham.backend.repository.CreatorRepository;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    private final CreatorRepository creatorRepository;
    private final BrandRepository brandRepository;

    public UserMapper(CreatorRepository creatorRepository, BrandRepository brandRepository) {
        this.creatorRepository = creatorRepository;
        this.brandRepository = brandRepository;
    }

    public UserResponse toResponse(User user) {
        CreatorProfilePayload creatorPayload = user.getRole().isCreator()
                ? creatorRepository.findById(user.getId()).map(this::toCreatorPayload).orElse(null)
                : null;
        BrandProfilePayload brandPayload = user.getRole().isBrand()
                ? brandRepository.findById(user.getId()).map(this::toBrandPayload).orElse(null)
                : null;

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name().toLowerCase(),
                user.getAvatarUrl() != null ? user.getAvatarUrl() : user.getImage(),
                user.getCreatorProgramStatus() != null
                        ? user.getCreatorProgramStatus().name().toLowerCase() : "none",
                user.getCity(),
                user.getPhone(),
                creatorPayload,
                brandPayload,
                user.isActive(),
                user.getCreatedAt()
        );
    }

    private CreatorProfilePayload toCreatorPayload(Creator creator) {
        return new CreatorProfilePayload(
                creator.getBio(),
                creator.getCategory(),
                creator.getTiktokUrl(),
                creator.getInstagramUrl(),
                creator.getYoutubeUrl(),
                creator.getFollowers(),
                creator.getAvgViews(),
                creator.getEngagementRate(),
                creator.getRating(),
                creator.getTotalReviews(),
                creator.isVerified()
        );
    }

    private BrandProfilePayload toBrandPayload(Brand brand) {
        return new BrandProfilePayload(
                brand.getName(),   // was getCompanyName()
                brand.getWebsite(),
                brand.getIndustry(),
                brand.getDescription(),
                brand.getBusinessVerificationStatus(),
                brand.getVerificationContactEmail(),
                brand.getVerificationPhoneNumber()
        );
    }
}
