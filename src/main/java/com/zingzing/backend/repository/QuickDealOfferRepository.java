package com.zingzing.backend.repository;

import com.zingzing.backend.entity.QuickDealOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuickDealOfferRepository extends JpaRepository<QuickDealOffer, UUID> {
    List<QuickDealOffer> findByConversationIdOrderByCreatedAtDesc(UUID conversationId);
}

