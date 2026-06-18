package com.zingzing.backend.entity;

import com.zingzing.backend.entity.enums.DealType;
import com.zingzing.backend.entity.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @Id
    private UUID id;

    @Column(name = "order_number", length = 20, unique = true)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    private ServicePackage servicePackage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Enumerated(EnumType.STRING)
    @Column(name = "deal_type", length = 30, nullable = false)
    @Builder.Default
    private DealType dealType = DealType.PAID;

    /** PKR amount in whole rupees (not paisa), nullable for barter-only deals. */
    @Column
    private Integer amount;

    @Column(name = "barter_details", columnDefinition = "text")
    private String barterDetails;

    @Column(columnDefinition = "text")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private int progress = 0;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "deadline_date")
    private OffsetDateTime deadlineDate;

    @Column(name = "barter_product_received", nullable = false)
    @Builder.Default
    private boolean barterProductReceived = false;

    @Column(name = "barter_expected_by")
    private OffsetDateTime barterExpectedBy;

    @Column(name = "idempotency_key", length = 64, unique = true)
    private String idempotencyKey;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Deliverable> deliverables = new ArrayList<>();
}
