package com.chamcham.backend.repository;

import com.chamcham.backend.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    List<Conversation> findByCreatorIdOrderByUpdatedAtDesc(UUID creatorId);

    List<Conversation> findByBrandIdOrderByUpdatedAtDesc(UUID brandId);

    Page<Conversation> findByCreatorIdOrderByUpdatedAtDesc(UUID creatorId, Pageable pageable);

    Page<Conversation> findByBrandIdOrderByUpdatedAtDesc(UUID brandId, Pageable pageable);

    Optional<Conversation> findByCreatorIdAndBrandId(UUID creatorId, UUID brandId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conversation c where c.id = :id")
    Optional<Conversation> findByIdForUpdate(@Param("id") UUID id);

    @Modifying
    @Query("update Conversation c set c.unreadCountCreator = 0, c.readByCreator = true where c.id = :id")
    int markReadForCreator(@Param("id") UUID id);

    @Modifying
    @Query("update Conversation c set c.unreadCountBrand = 0, c.readByBrand = true where c.id = :id")
    int markReadForBrand(@Param("id") UUID id);
}
