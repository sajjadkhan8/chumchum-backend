package com.zingzing.backend.repository;

import com.zingzing.backend.entity.DisputeCase;
import com.zingzing.backend.entity.enums.DisputeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DisputeCaseRepository extends JpaRepository<DisputeCase, UUID> {

    @Query("""
            select d from DisputeCase d
            join fetch d.order o
            join fetch o.creator
            join fetch o.brand
            join fetch o.servicePackage
            left join fetch d.assignedAdmin
            left join fetch d.refund
            where (:status is null or d.status = :status)
              and (
                cast(:search as string) is null
                or lower(d.title) like concat('%', lower(cast(:search as string)), '%')
                or lower(d.description) like concat('%', lower(cast(:search as string)), '%')
                or lower(o.creator.name) like concat('%', lower(cast(:search as string)), '%')
                or lower(o.brand.name) like concat('%', lower(cast(:search as string)), '%')
                or cast(o.id as string) like concat('%', cast(:search as string), '%')
              )
            order by d.createdAt desc
            """)
    Page<DisputeCase> searchForAdmin(@Param("search") String search,
                                     @Param("status") DisputeStatus status,
                                     Pageable pageable);

    boolean existsByOrderId(UUID orderId);

    Optional<DisputeCase> findByOrderId(UUID orderId);
}
