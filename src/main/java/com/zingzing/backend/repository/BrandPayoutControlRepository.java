package com.zingzing.backend.repository;

import com.zingzing.backend.entity.BrandPayoutControl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BrandPayoutControlRepository extends JpaRepository<BrandPayoutControl, UUID> {
}

