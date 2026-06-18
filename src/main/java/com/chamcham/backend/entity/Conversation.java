package com.chamcham.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "conversations",
        uniqueConstraints = @UniqueConstraint(name = "uk_conversation_pair", columnNames = {"creator_id", "brand_id"}))
public class Conversation extends BaseEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Column(name = "unread_count_creator", nullable = false)
    @Builder.Default
    private int unreadCountCreator = 0;

    @Column(name = "unread_count_brand", nullable = false)
    @Builder.Default
    private int unreadCountBrand = 0;

    @Column(name = "last_message_id")
    private UUID lastMessageId;

    /** Snapshot of last message content for conversation list display. */
    @Column(name = "last_message", length = 2000)
    private String lastMessage;
}
