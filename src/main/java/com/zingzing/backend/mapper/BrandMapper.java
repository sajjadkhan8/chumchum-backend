package com.zingzing.backend.mapper;

import com.zingzing.backend.dto.brand.BrandResponse;
import com.zingzing.backend.entity.Brand;
import org.springframework.stereotype.Component;

@Component
public class BrandMapper {

    private final ProfileUserMapper profileUserMapper;

    public BrandMapper(ProfileUserMapper profileUserMapper) {
        this.profileUserMapper = profileUserMapper;
    }

    public BrandResponse toResponse(Brand brand) {
        return toResponse(brand, false);
    }

    public BrandResponse toPublicResponse(Brand brand) {
        return toResponse(brand, true);
    }

    private BrandResponse toResponse(Brand brand, boolean publicView) {
        return new BrandResponse(
                brand.getId(),
                brand.getDisplayName(),
                brand.getWebsite(),
                brand.getIndustry(),
                brand.getDescription(),
                brand.getLogoUrl(),
                brand.getMonthlyBudget(),
                brand.getPreferredCreatorCategories(),
                brand.getTargetCities(),
                brand.getTargetPlatforms(),
                brand.getCampaignBudgetRange(),
                brand.getBusinessVerificationStatus(),
                publicView ? null : brand.getVerificationContactEmail(),
                publicView ? null : brand.getVerificationPhoneNumber(),
                brand.getPlanTier(),
                publicView ? profileUserMapper.toPublicResponse(brand) : profileUserMapper.toResponse(brand),
                brand.getCreatedAt(),
                brand.getUpdatedAt()
        );
    }
}
