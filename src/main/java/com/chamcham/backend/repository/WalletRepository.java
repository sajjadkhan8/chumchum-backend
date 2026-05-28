package com.chamcham.backend.repository;

import com.chamcham.backend.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Object> {
    Optional<Wallet> findByCreatorId(java.util.UUID creatorId);
}

