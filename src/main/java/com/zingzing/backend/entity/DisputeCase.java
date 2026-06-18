package com.zingzing.backend.entity;

import com.zingzing.backend.entity.enums.DisputeResolution;
import com.zingzing.backend.entity.enums.DisputeStatus;
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
@Table(name = "dispute_cases")
public class DisputeCase extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private DisputeStatus status = DisputeStatus.OPEN;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String priority = "normal";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_admin_id")
    private User assignedAdmin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private DisputeResolution resolution = DisputeResolution.NONE;

    @Column(name = "resolution_notes", columnDefinition = "text")
    private String resolutionNotes;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @OneToOne(mappedBy = "dispute", fetch = FetchType.LAZY)
    private PaymentRefund refund;
}
