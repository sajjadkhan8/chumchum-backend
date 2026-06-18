package com.zingzing.backend.repository;

import com.zingzing.backend.entity.PaymentAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PaymentAuditLogRepository extends JpaRepository<PaymentAuditLog, UUID> {

	@Query("""
			select p from PaymentAuditLog p
			join fetch p.actor
			left join fetch p.brand
			where (:action is null or lower(p.action) = lower(cast(:action as string)))
			  and (:brandId is null or p.brand.id = :brandId)
			  and (
				cast(:search as string) is null
				or lower(p.action) like concat('%', lower(cast(:search as string)), '%')
				or lower(p.targetType) like concat('%', lower(cast(:search as string)), '%')
				or lower(coalesce(p.targetId, '')) like concat('%', lower(cast(:search as string)), '%')
				or lower(coalesce(p.details, '')) like concat('%', lower(cast(:search as string)), '%')
				or lower(p.actor.name) like concat('%', lower(cast(:search as string)), '%')
				or lower(coalesce(p.brand.name, '')) like concat('%', lower(cast(:search as string)), '%')
			  )
			order by p.createdAt desc
			""")
	Page<PaymentAuditLog> searchForAdmin(@Param("search") String search,
										 @Param("action") String action,
										 @Param("brandId") UUID brandId,
										 Pageable pageable);
}

