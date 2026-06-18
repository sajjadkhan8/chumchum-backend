package com.zingzing.backend.repository;

import com.zingzing.backend.entity.Transaction;
import com.zingzing.backend.entity.enums.TransactionStatus;
import com.zingzing.backend.entity.enums.TransactionType;
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

    @Query("select coalesce(sum(t.amount), 0) from Transaction t where t.creator.id = :creatorId and t.type = :type and t.status = :status")
    long sumByCreatorAndTypeAndStatus(@Param("creatorId") UUID creatorId,
                                      @Param("type") TransactionType type,
                                      @Param("status") TransactionStatus status);

    @Query("select t from Transaction t join fetch t.creator c " +
           "where (:search is null or lower(c.name) like lower(concat('%', cast(:search as string), '%')) " +
           "  or lower(t.description) like lower(concat('%', cast(:search as string), '%'))) " +
           "and (:type is null or t.type = :type) " +
           "and (:status is null or t.status = :status) " +
           "order by t.createdAt desc")
    Page<Transaction> searchForAdmin(@Param("search") String search,
                                     @Param("type") TransactionType type,
                                     @Param("status") TransactionStatus status,
                                     Pageable pageable);

    @Query("select count(t) from Transaction t where t.status = :status")
    long countByStatus(@Param("status") TransactionStatus status);

    @Query("select coalesce(sum(t.amount), 0) from Transaction t where t.type = :type and t.status = :status")
    long sumByTypeAndStatus(@Param("type") TransactionType type, @Param("status") TransactionStatus status);
}
