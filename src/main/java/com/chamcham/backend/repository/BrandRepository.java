package com.chamcham.backend.repository;

import com.chamcham.backend.entity.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID> {

    @Query("""
            select b from Brand b
            where (cast(:search as string) is null
                   or lower(b.name) like concat('%', lower(cast(:search as string)), '%')
                   or lower(b.email) like concat('%', lower(cast(:search as string)), '%')
                   or lower(b.username) like concat('%', lower(cast(:search as string)), '%'))
              and (
                :verificationStatus is null
                or (:verificationStatus = 'pending' and (b.businessVerificationStatus is null or lower(b.businessVerificationStatus) = 'pending'))
                or lower(b.businessVerificationStatus) = :verificationStatus
              )
            order by b.createdAt desc
            """)
    Page<Brand> searchForAdmin(@Param("search") String search,
                               @Param("verificationStatus") String verificationStatus,
                               Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO core.brands (id, name, website, industry, description)
            VALUES (:id, :name, :website, :industry, :description)
            """, nativeQuery = true)
    int insertProfile(
            @Param("id") UUID id,
            @Param("name") String name,
            @Param("website") String website,
            @Param("industry") String industry,
            @Param("description") String description
    );
}
