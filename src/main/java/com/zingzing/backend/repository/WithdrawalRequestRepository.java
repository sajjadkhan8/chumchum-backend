package com.zingzing.backend.repository;

import com.zingzing.backend.entity.WithdrawalRequest;
import com.zingzing.backend.entity.enums.WithdrawalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, UUID> {
    Page<WithdrawalRequest> findByCreatorIdOrderByCreatedAtDesc(UUID creatorId, Pageable pageable);

    @Query("select w from WithdrawalRequest w join fetch w.creator c join fetch w.payoutMethod pm " +
           "where (:search is null or lower(c.name) like lower(concat('%', cast(:search as string), '%'))) " +
           "and (:status is null or w.status = :status) " +
           "order by w.createdAt desc")
    Page<WithdrawalRequest> searchForAdmin(@Param("search") String search,
                                           @Param("status") WithdrawalStatus status,
                                           Pageable pageable);

    @Query("select count(w) from WithdrawalRequest w where w.status = :status")
    long countByStatus(@Param("status") WithdrawalStatus status);

    @Query("select coalesce(sum(w.amount), 0) from WithdrawalRequest w where w.status = :status")
    long sumAmountByStatus(@Param("status") WithdrawalStatus status);
}

