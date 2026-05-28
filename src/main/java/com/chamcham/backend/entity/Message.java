package com.chamcham.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
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

    @Column(length = 50, name = "sender_type")
    private String senderType;

    @Column(length = 2000, name = "content")
    private String content;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private MessageType type = MessageType.TEXT;

    @Column(name = "is_read")
    @Builder.Default
    private boolean isRead = false;

    @Column(length = 500, name = "attachment_url")
    private String attachmentUrl;

    @Column(length = 100, name = "offer_deal_type")
    private String offerDealType;

    @Column(name = "offer_amount", precision = 10, scale = 2)
    private BigDecimal offerAmount;

    @Column(length = 1000, name = "offer_barter_details")
    private String offerBarterDetails;

    @Column(length = 100, name = "offer_barter_category")
    private String offerBarterCategory;

    @Column(name = "offer_estimated_barter_value", precision = 10, scale = 2)
    private BigDecimal offerEstimatedBarterValue;

    @Column(length = 1000, name = "offer_creator_expectation")
    private String offerCreatorExpectation;

    @Column(length = 2000, name = "offer_message")
    private String offerMessage;

    @Column(length = 50, name = "offer_status")
    private String offerStatus;

    public enum MessageType {
        TEXT,
        OFFER,
        SYSTEM,
        ATTACHMENT
    }
}

