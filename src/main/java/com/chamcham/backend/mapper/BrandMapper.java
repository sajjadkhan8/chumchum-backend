package com.chamcham.backend.mapper;

import com.chamcham.backend.dto.brand.BrandResponse;
import com.chamcham.backend.entity.Brand;
import org.springframework.stereotype.Component;

@Component
public class BrandMapper {

    private final ProfileUserMapper profileUserMapper;

    public BrandMapper(ProfileUserMapper profileUserMapper) {
        this.profileUserMapper = profileUserMapper;
    }

    public BrandResponse toResponse(Brand brand) {
        return new BrandResponse(
                brand.getId(),
                brand.getCompanyName(),
                brand.getWebsite(),
                brand.getIndustry(),
                brand.getDescription(),
                profileUserMapper.toResponse(brand.getUser()),
                brand.getCreatedAt(),
                brand.getUpdatedAt()
        );
    }
}

