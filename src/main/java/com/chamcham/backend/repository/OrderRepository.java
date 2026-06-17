package com.chamcham.backend.repository;

import com.chamcham.backend.entity.Order;
import com.chamcham.backend.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query("""
            select o from Order o
            join fetch o.servicePackage
            join fetch o.creator
            join fetch o.brand
            where o.creator.id = :userId or o.brand.id = :userId
            order by o.createdAt desc
            """)
    List<Order> findAllByParticipant(@Param("userId") UUID userId);

    @Query("""
            select o from Order o
            join fetch o.servicePackage
            join fetch o.creator
            join fetch o.brand
            where o.creator.id = :creatorId
            order by o.createdAt desc
            """)
    List<Order> findByCreatorIdOrderByCreatedAtDesc(@Param("creatorId") UUID creatorId);

    @Query("""
            select o from Order o
            join fetch o.servicePackage
            join fetch o.creator
            join fetch o.brand
            where o.brand.id = :brandId
            order by o.createdAt desc
            """)
    List<Order> findByBrandIdOrderByCreatedAtDesc(@Param("brandId") UUID brandId);

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

    @Query("""
            select distinct o from Order o
            join fetch o.servicePackage
            join fetch o.creator
            join fetch o.brand
            left join fetch o.deliverables
            order by o.createdAt desc
            """)
    List<Order> findAllForAdmin();

    @Query("""
            select distinct o from Order o
            join fetch o.servicePackage sp
            join fetch o.creator
            join fetch o.brand
            where (:status is null or o.status = :status)
              and (:search is null or lower(o.creator.name) like concat('%', lower(:search), '%')
                   or lower(sp.title) like concat('%', lower(:search), '%'))
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
            where o.status = com.chamcham.backend.entity.enums.OrderStatus.COMPLETED
            group by sp.category
            order by cnt desc
            """)
    List<Object[]> countCompletedOrdersByCategory();

    long countByStatus(OrderStatus status);

    @Query("select coalesce(sum(o.amount), 0) from Order o where o.status = :status and o.amount is not null")
    long sumAmountByStatus(@Param("status") OrderStatus status);

    @Query("select coalesce(sum(o.amount), 0) from Order o where o.amount is not null")
    long sumTotalGmv();

    @Query("select count(o) from Order o where o.creator.id = :creatorId and o.status = :status")
    long countByCreatorIdAndStatus(@Param("creatorId") UUID creatorId, @Param("status") OrderStatus status);
}
