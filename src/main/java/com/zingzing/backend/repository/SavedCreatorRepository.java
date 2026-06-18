package com.zingzing.backend.repository;

import com.zingzing.backend.entity.SavedCreator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedCreatorRepository extends JpaRepository<SavedCreator, SavedCreator.SavedCreatorId> {
    List<SavedCreator> findByBrandId(UUID brandId);
    @Query("""
            select saved from SavedCreator saved
            join fetch saved.creator
            where saved.brand.id = :brandId
            order by saved.savedAt desc
            """)
    List<SavedCreator> findByBrandIdWithCreator(@Param("brandId") UUID brandId);
    long countByBrandId(UUID brandId);
    Optional<SavedCreator> findByBrandIdAndCreatorId(UUID brandId, UUID creatorId);
    boolean existsByBrandIdAndCreatorId(UUID brandId, UUID creatorId);
    void deleteByBrandIdAndCreatorId(UUID brandId, UUID creatorId);
}
