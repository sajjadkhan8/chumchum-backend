package com.chamcham.backend.repository;

import com.chamcham.backend.entity.AmbassadorScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AmbassadorScoreRepository extends JpaRepository<AmbassadorScore, Object> {
    Optional<AmbassadorScore> findByCreatorId(java.util.UUID creatorId);
}

