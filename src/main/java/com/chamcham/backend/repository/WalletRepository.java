package com.chamcham.backend.repository;

import com.chamcham.backend.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    Optional<Wallet> findByCreatorId(UUID creatorId);

    @Modifying
    @Query(value = """
            insert into wallets (creator_id, total_earned, available_balance, pending_balance, updated_at)
            values (:creatorId, :amount, :amount, 0, now())
            on conflict (creator_id) do update set
                total_earned = wallets.total_earned + excluded.total_earned,
                available_balance = wallets.available_balance + excluded.available_balance,
                updated_at = now()
            """, nativeQuery = true)
    void creditCreatorEarnings(@Param("creatorId") UUID creatorId, @Param("amount") int amount);
}
