package com.chamcham.backend.repository;

import com.chamcham.backend.entity.AmbassadorScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AmbassadorScoreRepository extends JpaRepository<AmbassadorScore, UUID> {
    Optional<AmbassadorScore> findByCreatorId(UUID creatorId);
}

