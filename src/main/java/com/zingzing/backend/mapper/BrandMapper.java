package com.zingzing.backend.mapper;

import com.zingzing.backend.dto.brand.BrandResponse;
import com.zingzing.backend.entity.Brand;
import com.zingzing.backend.entity.enums.OrderStatus;
import com.zingzing.backend.repository.BrandCampaignRepository;
import com.zingzing.backend.repository.OrderRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BrandMapper {

    private static final List<OrderStatus> ACTIVE_ORDER_STATUSES = List.of(
            OrderStatus.PENDING, OrderStatus.ACCEPTED, OrderStatus.IN_PROGRESS,
            OrderStatus.DELIVERED, OrderStatus.REVIEW, OrderStatus.REVISION
    );

    private final ProfileUserMapper profileUserMapper;
    private final BrandCampaignRepository brandCampaignRepository;
    private final OrderRepository orderRepository;

    public BrandMapper(ProfileUserMapper profileUserMapper,
                       BrandCampaignRepository brandCampaignRepository,
                       OrderRepository orderRepository) {
        this.profileUserMapper = profileUserMapper;
        this.brandCampaignRepository = brandCampaignRepository;
        this.orderRepository = orderRepository;
    }

    public BrandResponse toResponse(Brand brand) {
        return toResponse(brand, false);
    }

    public BrandResponse toPublicResponse(Brand brand) {
        return toResponse(brand, true);
    }

    private BrandResponse toResponse(Brand brand, boolean publicView) {
        long totalCampaigns = brandCampaignRepository.countByBrandId(brand.getId());
        long activeOrders = orderRepository.countByBrandIdAndStatusIn(brand.getId(), ACTIVE_ORDER_STATUSES);
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
                brand.getBrandRating(),
                brand.getBrandTotalReviews(),
                brand.getCompanySize(),
                publicView ? null : brand.getContactName(),
                publicView ? null : brand.getContactEmail(),
                publicView ? null : brand.getContactPhone(),
                publicView ? profileUserMapper.toPublicResponse(brand) : profileUserMapper.toResponse(brand),
                brand.getCreatedAt(),
                brand.getUpdatedAt(),
                totalCampaigns,
                activeOrders
        );
    }
}
