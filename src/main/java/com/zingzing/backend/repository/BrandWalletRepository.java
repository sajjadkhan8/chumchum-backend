package com.zingzing.backend.repository;

import com.zingzing.backend.entity.BrandWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface BrandWalletRepository extends JpaRepository<BrandWallet, UUID> {

    /** Atomically moves :amount from walletBalance to pendingEscrow. Returns 1 on success, 0 if insufficient funds. */
    @Modifying
    @Query("update BrandWallet w set w.walletBalance = w.walletBalance - :amount, w.pendingEscrow = w.pendingEscrow + :amount where w.brandId = :brandId and w.walletBalance >= :amount")
    int holdEscrow(@Param("brandId") UUID brandId, @Param("amount") int amount);

    /** Decrements pendingEscrow when order payment is released to creator. */
    @Modifying
    @Query("update BrandWallet w set w.pendingEscrow = w.pendingEscrow - :amount where w.brandId = :brandId")
    void releaseEscrow(@Param("brandId") UUID brandId, @Param("amount") int amount);

    /** Returns escrowed amount back to walletBalance on order cancellation. */
    @Modifying
    @Query("update BrandWallet w set w.pendingEscrow = w.pendingEscrow - :amount, w.walletBalance = w.walletBalance + :amount where w.brandId = :brandId")
    void refundEscrow(@Param("brandId") UUID brandId, @Param("amount") int amount);
}
