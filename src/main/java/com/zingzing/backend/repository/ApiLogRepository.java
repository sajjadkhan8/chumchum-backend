package com.zingzing.backend.repository;

import com.zingzing.backend.entity.ApiLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ApiLogRepository extends JpaRepository<ApiLog, UUID> {

    @Query("""
            select l from ApiLog l
            where (:service is null or lower(l.service) = lower(cast(:service as string)))
              and (:success is null
                   or (:success = true and l.statusCode < 400)
                   or (:success = false and l.statusCode >= 400))
            order by l.createdAt desc
            """)
    Page<ApiLog> search(@Param("service") String service, @Param("success") Boolean success, Pageable pageable);
}
