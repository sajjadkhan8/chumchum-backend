package com.chamcham.backend.repository;

import com.chamcham.backend.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    List<Conversation> findBySellerIdOrderByUpdatedAtDesc(UUID sellerId);

    List<Conversation> findByBrandIdOrderByUpdatedAtDesc(UUID brandId);

    Optional<Conversation> findBySellerIdAndBrandId(UUID sellerId, UUID brandId);
}

