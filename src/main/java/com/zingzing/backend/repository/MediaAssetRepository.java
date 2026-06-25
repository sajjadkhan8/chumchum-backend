package com.zingzing.backend.repository;

import com.zingzing.backend.entity.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.Optional;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {

    long countByOwner_IdAndDeletedFalse(UUID ownerId);

    @Query("select coalesce(sum(m.bytes), 0) from MediaAsset m where m.owner.id = :ownerId and m.deleted = false")
    long sumBytesByOwner(@Param("ownerId") UUID ownerId);

    long countByOwner_IdAndEntityTypeAndEntityIdAndDeletedFalse(UUID ownerId, String entityType, UUID entityId);

    Optional<MediaAsset> findByAppPathAndDeletedFalse(String appPath);

    @Query("select coalesce(sum(m.bytes), 0) from MediaAsset m where m.owner.id = :ownerId and m.entityType = :entityType and m.entityId = :entityId and m.deleted = false")
    long sumBytesByOwnerAndEntity(@Param("ownerId") UUID ownerId, @Param("entityType") String entityType, @Param("entityId") UUID entityId);
}
