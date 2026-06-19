package com.zingzing.backend.service;

import com.zingzing.backend.dto.brand.BrandCreateRequest;
import com.zingzing.backend.dto.brand.BrandResponse;
import com.zingzing.backend.dto.brand.BrandUpdateRequest;
import com.zingzing.backend.entity.Brand;
import com.zingzing.backend.entity.User;
import com.zingzing.backend.entity.enums.UserRole;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.mapper.BrandMapper;
import com.zingzing.backend.repository.BrandRepository;
import com.zingzing.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BrandService {

    private final BrandRepository brandRepository;
    private final UserRepository userRepository;
    private final BrandMapper brandMapper;

    public BrandService(BrandRepository brandRepository, UserRepository userRepository, BrandMapper brandMapper) {
        this.brandRepository = brandRepository;
        this.userRepository = userRepository;
        this.brandMapper = brandMapper;
    }

    @Transactional
    public BrandResponse create(BrandCreateRequest request) {
        if (request.userId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "userId is required");
        }

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (!user.getRole().isBrand()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "User role must be BRAND");
        }

        if (brandRepository.findById(user.getId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Brand profile already exists for this user");
        }

        int rows = brandRepository.insertProfile(
                user.getId(),
                request.companyName(),
                null,   // logoUrl
                request.website(),
                request.industry(),
                request.description(),
                null, null, null, null, null, null, null, null, null, null, null, null
        );

        if (rows != 1) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create brand profile");
        }

        Brand created = brandRepository.findById(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Brand created but not loadable"));
        return brandMapper.toResponse(created);
    }

    @Transactional
    public List<BrandResponse> getAll() {
        return brandRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream().map(brandMapper::toPublicResponse).toList();
    }

    @Transactional
    public BrandResponse getById(UUID brandId) {
        return brandMapper.toPublicResponse(findBrand(brandId));
    }

    @Transactional
    public BrandResponse getByUserId(UUID actorUserId, UserRole actorRole, UUID userId) {
        validateOwnerOrAdmin(actorUserId, actorRole, userId);
        Brand brand = brandRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Brand profile not found"));
        return brandMapper.toResponse(brand);
    }

    @Transactional
    public BrandResponse update(UUID actorUserId, UserRole actorRole, UUID brandId, BrandUpdateRequest request) {
        Brand brand = findBrand(brandId);
        validateOwnerOrAdmin(actorUserId, actorRole, brand.getId());

        if (request.companyName() != null && !request.companyName().isBlank()) brand.setName(request.companyName());
        if (request.website()     != null) brand.setWebsite(request.website());
        if (request.industry()    != null) brand.setIndustry(request.industry());
        if (request.description() != null) brand.setDescription(request.description());
        if (request.logoUrl()     != null) brand.setLogoUrl(request.logoUrl());
        if (request.monthlyBudget() != null) brand.setMonthlyBudget(request.monthlyBudget());
        if (request.preferredCreatorCategories() != null) brand.setPreferredCreatorCategories(request.preferredCreatorCategories());
        if (request.targetCities() != null) brand.setTargetCities(request.targetCities());
        if (request.targetPlatforms() != null) brand.setTargetPlatforms(request.targetPlatforms());
        if (request.campaignBudgetRange() != null) brand.setCampaignBudgetRange(request.campaignBudgetRange());
        if (request.businessVerificationStatus() != null) brand.setBusinessVerificationStatus(request.businessVerificationStatus());
        if (request.verificationContactEmail() != null) brand.setVerificationContactEmail(request.verificationContactEmail());
        if (request.verificationPhoneNumber() != null) brand.setVerificationPhoneNumber(request.verificationPhoneNumber());
        if (request.city()         != null) brand.setCity(request.city());
        if (request.companySize()  != null) brand.setCompanySize(request.companySize());
        if (request.contactName()  != null) brand.setContactName(request.contactName());
        if (request.contactEmail() != null) brand.setContactEmail(request.contactEmail());
        if (request.contactPhone() != null) brand.setContactPhone(request.contactPhone());

        return brandMapper.toResponse(brandRepository.save(brand));
    }

    private Brand findBrand(UUID brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Brand profile not found"));
    }

    private void validateOwnerOrAdmin(UUID actorUserId, UserRole actorRole, UUID resourceUserId) {
        if (!actorRole.isAdmin() && !actorUserId.equals(resourceUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only manage your own brand profile");
        }
    }
}
