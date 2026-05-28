package com.chamcham.backend.service;

import com.chamcham.backend.entity.Creator;
import com.chamcham.backend.entity.SavedCreator;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.repository.BrandRepository;
import com.chamcham.backend.repository.CreatorRepository;
import com.chamcham.backend.repository.SavedCreatorRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SavedCreatorService {

    private final SavedCreatorRepository savedCreatorRepository;
    private final BrandRepository brandRepository;
    private final CreatorRepository creatorRepository;

    public SavedCreatorService(SavedCreatorRepository savedCreatorRepository,
                               BrandRepository brandRepository,
                               CreatorRepository creatorRepository) {
        this.savedCreatorRepository = savedCreatorRepository;
        this.brandRepository = brandRepository;
        this.creatorRepository = creatorRepository;
    }

    public List<Creator> getSaved(UUID brandUserId, UserRole role) {
        requireBrand(role);
        return savedCreatorRepository.findByBrandId(brandUserId)
                .stream().map(SavedCreator::getCreator).toList();
    }

    @Transactional
    public void save(UUID brandUserId, UUID creatorId, UserRole role) {
        requireBrand(role);
        if (savedCreatorRepository.existsByBrandIdAndCreatorId(brandUserId, creatorId)) return;

        var brand = brandRepository.findById(brandUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Brand not found"));
        var creator = creatorRepository.findById(creatorId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator not found"));

        savedCreatorRepository.save(SavedCreator.builder().brand(brand).creator(creator).build());
    }

    @Transactional
    public void unsave(UUID brandUserId, UUID creatorId, UserRole role) {
        requireBrand(role);
        savedCreatorRepository.deleteByBrandIdAndCreatorId(brandUserId, creatorId);
    }

    public boolean isSaved(UUID brandUserId, UUID creatorId) {
        return savedCreatorRepository.existsByBrandIdAndCreatorId(brandUserId, creatorId);
    }

    private void requireBrand(UserRole role) {
        if (!role.isBrand()) throw new ApiException(HttpStatus.FORBIDDEN, "Only brands can save creators");
    }
}

