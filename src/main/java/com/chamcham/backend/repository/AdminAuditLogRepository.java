package com.chamcham.backend.repository;

import com.chamcham.backend.entity.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, UUID> {

    @Query("""
            select a from AdminAuditLog a
            join fetch a.admin
            where (:action is null or lower(a.action) = lower(cast(:action as string)))
              and (
                cast(:search as string) is null
                or lower(a.action) like concat('%', lower(cast(:search as string)), '%')
                or lower(a.targetType) like concat('%', lower(cast(:search as string)), '%')
                or lower(coalesce(a.targetId, '')) like concat('%', lower(cast(:search as string)), '%')
                or lower(coalesce(a.details, '')) like concat('%', lower(cast(:search as string)), '%')
                or lower(a.admin.name) like concat('%', lower(cast(:search as string)), '%')
              )
            order by a.createdAt desc
            """)
    Page<AdminAuditLog> searchForAdmin(@Param("search") String search,
                                       @Param("action") String action,
                                       Pageable pageable);
}
