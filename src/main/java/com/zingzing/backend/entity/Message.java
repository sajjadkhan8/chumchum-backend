package com.zingzing.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "messages")
public class Message extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "sender_type", length = 20)
    private String senderType;   // "creator" | "brand"

    /** Text content (null for offer/attachment-only messages). */
    @Column(length = 2000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    @Builder.Default
    private MessageType type = MessageType.TEXT;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Column(name = "attachment_original_name", length = 255)
    private String attachmentOriginalName;

    // ---- Embedded offer snapshot (quick deal data stored inline) ----
    @Column(name = "offer_deal_type", length = 30)
    private String offerDealType;

    @Column(name = "offer_amount")
    private Integer offerAmount;

    @Column(name = "offer_barter_details", columnDefinition = "text")
    private String offerBarterDetails;

    @Column(name = "offer_barter_category", length = 100)
    private String offerBarterCategory;

    @Column(name = "offer_estimated_barter_value")
    private Integer offerEstimatedBarterValue;

    @Column(name = "offer_creator_expectation", columnDefinition = "text")
    private String offerCreatorExpectation;

    @Column(name = "offer_message", columnDefinition = "text")
    private String offerMessage;

    @Column(name = "offer_status", length = 30)
    private String offerStatus;

    @OneToOne(mappedBy = "messageEntity", fetch = FetchType.LAZY)
    private QuickDealOffer quickDealOffer;

    /** Legacy column kept for DB compat. */
    @Column(length = 2000)
    private String description;

    public enum MessageType {
        TEXT, OFFER, SYSTEM, ATTACHMENT
    }
}
