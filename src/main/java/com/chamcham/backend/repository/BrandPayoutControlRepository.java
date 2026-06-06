package com.chamcham.backend.repository;

import com.chamcham.backend.entity.BrandPayoutControl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BrandPayoutControlRepository extends JpaRepository<BrandPayoutControl, UUID> {
}

