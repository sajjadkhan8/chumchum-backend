package com.chamcham.backend.repository;

import com.chamcham.backend.entity.BrandWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BrandWalletRepository extends JpaRepository<BrandWallet, UUID> {
}

