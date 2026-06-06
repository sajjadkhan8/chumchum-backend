package com.chamcham.backend.repository;

import com.chamcham.backend.entity.Transaction;
import com.chamcham.backend.entity.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Page<Transaction> findByCreatorIdOrderByCreatedAtDesc(UUID creatorId, Pageable pageable);

    boolean existsByOrderIdAndType(UUID orderId, TransactionType type);

    Optional<Transaction> findByOrderIdAndType(UUID orderId, TransactionType type);

    @Query("select coalesce(sum(t.amount), 0) from Transaction t where t.creator.id = :creatorId and t.type = :type and t.status = 'COMPLETED'")
    long sumCompletedByCreatorAndType(@Param("creatorId") UUID creatorId, @Param("type") TransactionType type);
}
