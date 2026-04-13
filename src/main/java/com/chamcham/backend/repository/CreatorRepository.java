package com.chamcham.backend.repository;

import com.chamcham.backend.entity.Creator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CreatorRepository extends JpaRepository<Creator, UUID> {

    Optional<Creator> findByUserId(UUID userId);
}
