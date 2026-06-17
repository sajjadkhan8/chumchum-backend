package com.chamcham.backend.entity;

import com.chamcham.backend.entity.enums.TransactionStatus;
import com.chamcham.backend.entity.enums.TransactionStatusConverter;
import com.chamcham.backend.entity.enums.TransactionType;
import com.chamcham.backend.entity.enums.TransactionTypeConverter;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Convert(converter = TransactionTypeConverter.class)
    @Column(nullable = false, length = 30)
    private TransactionType type;

    /** Positive = credit, negative = debit (SAR integer). */
    @Column(nullable = false)
    private int amount;

    @Column(nullable = false, length = 300)
    private String description;

    @Convert(converter = TransactionStatusConverter.class)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
