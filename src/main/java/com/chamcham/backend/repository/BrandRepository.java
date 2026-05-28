package com.chamcham.backend.repository;

import com.chamcham.backend.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID> {

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
