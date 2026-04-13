package com.chamcham.backend.mapper;

import com.chamcham.backend.dto.user.BrandProfilePayload;
import com.chamcham.backend.dto.user.CreatorProfilePayload;
import com.chamcham.backend.dto.user.UserResponse;
import com.chamcham.backend.entity.Brand;
import com.chamcham.backend.entity.Creator;
import com.chamcham.backend.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getImage(),
                user.getCity(),
                user.getPhone(),
                user.getRole(),
                user.getRole().isCreator(),
                user.isActive(),
                toCreatorPayload(user.getCreator()),
                toBrandPayload(user.getBrand()),
                user.getCreatedAt()
        );
    }

    private CreatorProfilePayload toCreatorPayload(Creator creator) {
        if (creator == null) {
            return null;
        }

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
                creator.getTotalReviews()
        );
    }

    private BrandProfilePayload toBrandPayload(Brand brand) {
        if (brand == null) {
            return null;
        }

        return new BrandProfilePayload(
                brand.getCompanyName(),
                brand.getWebsite(),
                brand.getIndustry(),
                brand.getDescription()
        );
    }
}

