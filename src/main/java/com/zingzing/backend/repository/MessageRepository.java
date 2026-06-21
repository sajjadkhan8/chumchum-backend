package com.zingzing.backend.repository;

import com.zingzing.backend.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    List<Message> findByConversationIdAndCreatedAtAfterOrderByCreatedAtAsc(UUID conversationId, Instant createdAt);

    Optional<Message> findByAttachmentUrl(String attachmentUrl);

    @Modifying
    @Query("update Message m set m.isRead = true where m.conversation.id = :conversationId and m.sender.id <> :userId and m.isRead = false")
    int markIncomingRead(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);
}
