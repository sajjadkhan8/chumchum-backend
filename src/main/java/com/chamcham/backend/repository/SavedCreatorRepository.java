package com.chamcham.backend.repository;

import com.chamcham.backend.entity.SavedCreator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedCreatorRepository extends JpaRepository<SavedCreator, SavedCreator.SavedCreatorId> {
    List<SavedCreator> findByBrandId(UUID brandId);
    Optional<SavedCreator> findByBrandIdAndCreatorId(UUID brandId, UUID creatorId);
    boolean existsByBrandIdAndCreatorId(UUID brandId, UUID creatorId);
    void deleteByBrandIdAndCreatorId(UUID brandId, UUID creatorId);
}

