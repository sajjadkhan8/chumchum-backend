package com.zingzing.backend.repository;

import com.zingzing.backend.entity.Conversation;
import com.zingzing.backend.entity.Conversation.ContextType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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

    @EntityGraph(attributePaths = {"creator", "brand"})
    Page<Conversation> findByCreatorIdOrderByUpdatedAtDesc(UUID creatorId, Pageable pageable);

    @EntityGraph(attributePaths = {"creator", "brand"})
    Page<Conversation> findByBrandIdOrderByUpdatedAtDesc(UUID brandId, Pageable pageable);

    @EntityGraph(attributePaths = {"creator", "brand"})
    Optional<Conversation> findByCreatorIdAndBrandIdAndContextTypeAndContextIdIsNull(UUID creatorId, UUID brandId, ContextType contextType);

    @EntityGraph(attributePaths = {"creator", "brand"})
    Optional<Conversation> findByContextTypeAndContextId(ContextType contextType, UUID contextId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conversation c where c.id = :id")
    Optional<Conversation> findByIdForUpdate(@Param("id") UUID id);

    @Modifying
    @Query("update Conversation c set c.unreadCountCreator = 0 where c.id = :id")
    int markReadForCreator(@Param("id") UUID id);

    @Modifying
    @Query("update Conversation c set c.unreadCountBrand = 0 where c.id = :id")
    int markReadForBrand(@Param("id") UUID id);
}
