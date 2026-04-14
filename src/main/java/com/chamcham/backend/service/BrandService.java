package com.chamcham.backend.service;

import com.chamcham.backend.dto.brand.BrandCreateRequest;
import com.chamcham.backend.dto.brand.BrandResponse;
import com.chamcham.backend.dto.brand.BrandUpdateRequest;
import com.chamcham.backend.entity.Brand;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.BrandMapper;
import com.chamcham.backend.repository.BrandRepository;
import com.chamcham.backend.repository.UserRepository;
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

        int insertedRows = brandRepository.insertProfile(
                user.getId(),
                request.companyName(),
                request.website(),
                request.industry(),
                request.description()
        );

        if (insertedRows != 1) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create brand profile");
        }

        Brand created = brandRepository.findById(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Brand profile created but could not be loaded"));
        return brandMapper.toResponse(created);
    }

    @Transactional
    public List<BrandResponse> getAll() {
        return brandRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(brandMapper::toResponse)
                .toList();
    }

    @Transactional
    public BrandResponse getById(UUID brandId) {
        return brandMapper.toResponse(findBrand(brandId));
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

        if (request.companyName() != null && !request.companyName().isBlank()) {
            brand.setCompanyName(request.companyName());
        }
        if (request.website() != null) {
            brand.setWebsite(request.website());
        }
        if (request.industry() != null) {
            brand.setIndustry(request.industry());
        }
        if (request.description() != null) {
            brand.setDescription(request.description());
        }

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
