package com.chamcham.backend.repository;

import com.chamcham.backend.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    Optional<Message> findByAttachmentUrl(String attachmentUrl);

    @Modifying
    @Query("update Message m set m.isRead = true where m.conversation.id = :conversationId and m.sender.id <> :userId and m.isRead = false")
    int markIncomingRead(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);
}
