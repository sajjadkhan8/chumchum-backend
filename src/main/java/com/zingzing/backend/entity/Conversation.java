package com.zingzing.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "conversations")
public class Conversation extends BaseEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", length = 30, nullable = false)
    @Builder.Default
    private ContextType contextType = ContextType.GENERAL;

    @Column(name = "context_id")
    private UUID contextId;

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

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "cleared_at_creator")
    private Instant clearedAtCreator;

    @Column(name = "cleared_at_brand")
    private Instant clearedAtBrand;

    @Column(name = "blocked_at_creator")
    private Instant blockedAtCreator;

    @Column(name = "blocked_at_brand")
    private Instant blockedAtBrand;

    public enum ContextType {
        GENERAL,
        ORDER,
        DISPUTE,
        CAMPAIGN,
        OFFER,
        PAYMENT
    }
}
