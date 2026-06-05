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

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "deliverables")
public class Deliverable extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private DeliverableStatus status = DeliverableStatus.PENDING;

    @Column(length = 500, name = "file_url")
    private String fileUrl;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    public enum DeliverableStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        REVISION,
        REVIEW,
        APPROVED,
        pending,
        in_progress,
        completed,
        revision,
        review,
        approved
    }
}
