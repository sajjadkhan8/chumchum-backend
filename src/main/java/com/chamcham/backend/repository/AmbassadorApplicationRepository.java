package com.chamcham.backend.repository;

import com.chamcham.backend.entity.AmbassadorApplication;
import com.chamcham.backend.entity.enums.AmbassadorAppStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AmbassadorApplicationRepository extends JpaRepository<AmbassadorApplication, UUID> {
    Optional<AmbassadorApplication> findByCreatorId(UUID creatorId);
    Page<AmbassadorApplication> findByStatus(AmbassadorAppStatus status, Pageable pageable);

    @Query("""
            select a from AmbassadorApplication a
            join fetch a.creator c
            where (:status is null or a.status = :status)
              and (
                cast(:search as string) is null
                or lower(c.name) like concat('%', lower(cast(:search as string)), '%')
                or lower(c.email) like concat('%', lower(cast(:search as string)), '%')
                or lower(cast(c.id as string)) like concat('%', lower(cast(:search as string)), '%')
              )
            order by a.createdAt desc
            """)
    Page<AmbassadorApplication> searchForAdmin(@Param("search") String search,
                                               @Param("status") AmbassadorAppStatus status,
                                               Pageable pageable);
}
