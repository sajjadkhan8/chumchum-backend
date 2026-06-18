package com.zingzing.backend.repository;

import com.zingzing.backend.entity.CreatorPayoutPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CreatorPayoutPreferenceRepository extends JpaRepository<CreatorPayoutPreference, UUID> {
}

