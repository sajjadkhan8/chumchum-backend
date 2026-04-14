package com.chamcham.backend.mapper;

import com.chamcham.backend.dto.creator.CreatorResponse;
import com.chamcham.backend.entity.Creator;
import org.springframework.stereotype.Component;

@Component
public class CreatorMapper {

    private final ProfileUserMapper profileUserMapper;

    public CreatorMapper(ProfileUserMapper profileUserMapper) {
        this.profileUserMapper = profileUserMapper;
    }

    public CreatorResponse toResponse(Creator creator) {
        return new CreatorResponse(
                creator.getId(),
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
                profileUserMapper.toResponse(creator),
                creator.getCreatedAt(),
                creator.getUpdatedAt()
        );
    }
}
