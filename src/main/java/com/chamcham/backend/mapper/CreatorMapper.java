package com.chamcham.backend.mapper;

import com.chamcham.backend.dto.creator.CreatorResponse;
import com.chamcham.backend.dto.creator.ContentPreviewResponse;
import com.chamcham.backend.dto.creator.SocialAccountResponse;
import com.chamcham.backend.entity.ContentPreview;
import com.chamcham.backend.entity.Creator;
import com.chamcham.backend.entity.SocialAccount;
import com.chamcham.backend.repository.ContentPreviewRepository;
import com.chamcham.backend.repository.SocialAccountRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CreatorMapper {

    private final ProfileUserMapper profileUserMapper;
    private final SocialAccountRepository socialAccountRepository;
    private final ContentPreviewRepository contentPreviewRepository;

    public CreatorMapper(ProfileUserMapper profileUserMapper, SocialAccountRepository socialAccountRepository,
                         ContentPreviewRepository contentPreviewRepository) {
        this.profileUserMapper = profileUserMapper;
        this.socialAccountRepository = socialAccountRepository;
        this.contentPreviewRepository = contentPreviewRepository;
    }

    public CreatorResponse toResponse(Creator creator) {
        List<SocialAccountResponse> socialAccounts = socialAccountRepository.findByCreatorId(creator.getId()).stream()
                .map(this::toSocialAccountResponse)
                .toList();
        List<ContentPreviewResponse> contentPreviews = contentPreviewRepository.findByCreatorIdOrderByCreatedAtDesc(creator.getId()).stream()
                .map(this::toContentPreviewResponse)
                .toList();

        return new CreatorResponse(
                creator.getId(),
                creator.getName(),
                creator.getUsername(),
                creator.getEmail(),
                creator.getPhone(),
                creator.getCity(),
                creator.getAvatarUrl(),
                creator.getBio(),
                creator.getCategory(),
                creator.getCoverImageUrl(),
                creator.getWebsite(),
                creator.getNiche(),
                creator.getAvailabilityStatus(),
                creator.getResponseTime(),
                creator.getMinPrice(),
                creator.getMaxPrice(),
                creator.isVerified(),
                creator.getBadgeLevel(),
                creator.isTrending(),
                creator.isFastResponder(),
                creator.getCompletedDeals(),
                creator.isAcceptsBarter(),
                creator.isAcceptsHybridDeals(),
                creator.getMinimumBudget(),
                creator.getPreferredIndustries(),
                creator.getLanguages(),
                creator.getCategories(),
                creator.getTiktokUrl(),
                creator.getInstagramUrl(),
                creator.getYoutubeUrl(),
                creator.getFacebookUrl(),
                creator.getFollowers(),
                creator.getAvgViews(),
                creator.getEngagementRate(),
                creator.getRating(),
                creator.getTotalReviews(),
                socialAccounts,
                contentPreviews,
                profileUserMapper.toResponse(creator),
                creator.getCreatedAt(),
                creator.getUpdatedAt()
        );
    }

    private SocialAccountResponse toSocialAccountResponse(SocialAccount socialAccount) {
        return new SocialAccountResponse(
                socialAccount.getId(),
                socialAccount.getPlatform(),
                socialAccount.getUsername(),
                socialAccount.getProfileUrl(),
                socialAccount.getFollowers(),
                socialAccount.getAvgViews(),
                socialAccount.getEngagementRate(),
                socialAccount.isVerified()
        );
    }

    private ContentPreviewResponse toContentPreviewResponse(ContentPreview preview) {
        return new ContentPreviewResponse(
                preview.getId(),
                preview.getType(),
                preview.getThumbnailUrl(),
                preview.getMediaUrl(),
                preview.getPlatform(),
                preview.getViews(),
                preview.getLikes()
        );
    }
}
