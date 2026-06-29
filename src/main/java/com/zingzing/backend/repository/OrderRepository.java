package com.zingzing.backend.repository;

import com.zingzing.backend.entity.Order;
import com.zingzing.backend.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query("""
            select o from Order o
            join fetch o.servicePackage
            join fetch o.creator
            join fetch o.brand
            where o.creator.id = :creatorId
            order by o.createdAt desc
            """)
    List<Order> findByCreatorIdOrderByCreatedAtDesc(@Param("creatorId") UUID creatorId);

    @Query(value = """
            select o from Order o
            join fetch o.servicePackage
            join fetch o.creator
            join fetch o.brand
            where o.creator.id = :creatorId
            order by o.createdAt desc
            """,
            countQuery = "select count(o) from Order o where o.creator.id = :creatorId")
    Page<Order> findByCreatorIdOrderByCreatedAtDesc(@Param("creatorId") UUID creatorId, Pageable pageable);

    @Query("""
            select o from Order o
            join fetch o.servicePackage
            join fetch o.creator
            join fetch o.brand
            where o.brand.id = :brandId
            order by o.createdAt desc
            """)
    List<Order> findByBrandIdOrderByCreatedAtDesc(@Param("brandId") UUID brandId);

    @Query(value = """
            select o from Order o
            join fetch o.servicePackage
            join fetch o.creator
            join fetch o.brand
            where o.brand.id = :brandId
            order by o.createdAt desc
            """,
            countQuery = "select count(o) from Order o where o.brand.id = :brandId")
    Page<Order> findByBrandIdOrderByCreatedAtDesc(@Param("brandId") UUID brandId, Pageable pageable);

    List<Order> findByCreatorIdAndStatusIn(UUID creatorId, List<OrderStatus> statuses);

    List<Order> findByBrandIdAndStatusIn(UUID brandId, List<OrderStatus> statuses);

    List<Order> findByCreatorIdAndStatus(UUID creatorId, OrderStatus status, Pageable pageable);

    long countByCreatorIdAndStatusIn(UUID creatorId, List<OrderStatus> statuses);

    long countByBrandIdAndStatusIn(UUID brandId, List<OrderStatus> statuses);

    @Query("select count(distinct o.brand.id) from Order o where o.creator.id = :creatorId and o.status = 'COMPLETED'")
    long countDistinctBrandsByCreatorAndCompleted(@Param("creatorId") UUID creatorId);

    @Query("select count(distinct o.creator.id) from Order o where o.brand.id = :brandId")
    long countDistinctCreatorsByBrand(@Param("brandId") UUID brandId);

    Optional<Order> findFirstByServicePackageName(String packageName);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    @Query("""
            select distinct o from Order o
            join fetch o.servicePackage sp
            join fetch o.creator
            join fetch o.brand
            where (:status is null or o.status = :status)
              and (:search is null or lower(o.creator.name) like concat('%', lower(cast(:search as string)), '%')
                   or lower(sp.title) like concat('%', lower(cast(:search as string)), '%'))
            order by o.createdAt desc
            """)
    Page<Order> findForAdminPaged(
            @Param("status") OrderStatus status,
            @Param("search") String search,
            Pageable pageable);

    @Query("""
            select sp.category, count(o) as cnt
            from Order o
            join o.servicePackage sp
            where o.status = com.zingzing.backend.entity.enums.OrderStatus.COMPLETED
            group by sp.category
            order by cnt desc
            """)
    List<Object[]> countCompletedOrdersByCategory();

    long countByStatus(OrderStatus status);

    @Query("select coalesce(sum(o.amount), 0) from Order o where o.status = :status and o.amount is not null")
    long sumAmountByStatus(@Param("status") OrderStatus status);

    @Query("select coalesce(sum(o.amount), 0) from Order o where o.amount is not null")
    long sumTotalGmv();

    @Query("""
            select o.amount from Order o
            where o.creator.id = :creatorId
              and o.status in (
                com.zingzing.backend.entity.enums.OrderStatus.DELIVERED,
                com.zingzing.backend.entity.enums.OrderStatus.REVIEW)
              and o.dealType <> com.zingzing.backend.entity.enums.DealType.BARTER
              and o.amount is not null
              and o.amount > 0
            """)
    List<Integer> findAwaitingApprovalPaidAmountsByCreatorId(@Param("creatorId") UUID creatorId);

    @Query("select count(o) from Order o where o.creator.id = :creatorId and o.status = :status")
    long countByCreatorIdAndStatus(@Param("creatorId") UUID creatorId, @Param("status") OrderStatus status);

    @Query("""
            select distinct o from Order o
            join fetch o.creator
            join fetch o.brand
            join fetch o.servicePackage
            left join fetch o.deliverables
            where o.id = :id
            """)
    Optional<Order> findByIdWithDetails(@Param("id") UUID id);

    @Query("""
            select o from Order o
            join fetch o.creator
            join fetch o.brand
            join fetch o.servicePackage
            where (o.dealType = com.zingzing.backend.entity.enums.DealType.BARTER
                   or o.dealType = com.zingzing.backend.entity.enums.DealType.HYBRID)
              and o.barterProductReceived = false
              and o.barterExpectedBy is not null
              and o.barterExpectedBy < :now
              and o.status in (com.zingzing.backend.entity.enums.OrderStatus.ACCEPTED,
                               com.zingzing.backend.entity.enums.OrderStatus.IN_PROGRESS,
                               com.zingzing.backend.entity.enums.OrderStatus.DELIVERED,
                               com.zingzing.backend.entity.enums.OrderStatus.REVIEW,
                               com.zingzing.backend.entity.enums.OrderStatus.REVISION)
            """)
    List<Order> findOverdueBarterOrders(@Param("now") OffsetDateTime now);

    // ── SLA metrics ──────────────────────────────────────────────────────────

    /** Orders past their deadline that are still active (not completed/cancelled). */
    @Query("""
            select count(o) from Order o
            where o.deadlineDate is not null
              and o.deadlineDate < :now
              and o.status not in (
                com.zingzing.backend.entity.enums.OrderStatus.COMPLETED,
                com.zingzing.backend.entity.enums.OrderStatus.CANCELLED)
            """)
    long countActiveOverdueOrders(@Param("now") OffsetDateTime now);

    /** Active orders (not completed/cancelled) pending for more than :thresholdHours hours. */
    @Query("""
            select count(o) from Order o
            where o.status = com.zingzing.backend.entity.enums.OrderStatus.PENDING
              and o.createdAt < :cutoff
            """)
    long countPendingBeyondThreshold(@Param("cutoff") Instant cutoff);

    /**
     * Among completed orders that had a deadline, how many were delivered on time
     * (deliveryDate <= deadlineDate cast to date).
     * Returns [onTimeCount, totalWithDeadline] as Object[].
     */
    @Query(value = """
            select
              count(case when o.delivery_date <= cast(o.deadline_date as date) then 1 end),
              count(*)
            from orders o
            where o.status = 'COMPLETED'
              and o.deadline_date is not null
              and o.delivery_date is not null
            """, nativeQuery = true)
    List<Object[]> countOnTimeVsTotal();

    /**
     * Average resolution time in hours for COMPLETED orders.
     * Resolution = updatedAt - createdAt.
     */
    @Query(value = """
            select extract(epoch from avg(o.updated_at - o.created_at)) / 3600
            from orders o
            where o.status = 'COMPLETED'
            """, nativeQuery = true)
    Double avgResolutionHours();

    @Query(value = """
            select o from Order o
            join fetch o.servicePackage sp
            join fetch o.creator
            join fetch o.brand
            where o.creator.id = :creatorId
              and (:status is null or o.status = :status)
              and (:search is null
                   or lower(sp.title) like concat('%', lower(cast(:search as string)), '%')
                   or lower(o.brand.name) like concat('%', lower(cast(:search as string)), '%')
                   or lower(o.orderNumber) like concat('%', lower(cast(:search as string)), '%'))
            order by o.createdAt desc
            """,
            countQuery = """
            select count(o) from Order o
            where o.creator.id = :creatorId
              and (:status is null or o.status = :status)
            """)
    Page<Order> findByCreatorIdFiltered(
            @Param("creatorId") UUID creatorId,
            @Param("status") OrderStatus status,
            @Param("search") String search,
            Pageable pageable);

    @Query(value = """
            select o from Order o
            join fetch o.servicePackage sp
            join fetch o.creator
            join fetch o.brand
            where o.brand.id = :brandId
              and (:status is null or o.status = :status)
              and (:search is null
                   or lower(sp.title) like concat('%', lower(cast(:search as string)), '%')
                   or lower(o.creator.name) like concat('%', lower(cast(:search as string)), '%')
                   or lower(o.orderNumber) like concat('%', lower(cast(:search as string)), '%'))
            order by o.createdAt desc
            """,
            countQuery = """
            select count(o) from Order o
            where o.brand.id = :brandId
              and (:status is null or o.status = :status)
            """)
    Page<Order> findByBrandIdFiltered(
            @Param("brandId") UUID brandId,
            @Param("status") OrderStatus status,
            @Param("search") String search,
            Pageable pageable);
}
