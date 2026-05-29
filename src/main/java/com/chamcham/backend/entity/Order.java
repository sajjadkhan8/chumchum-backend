package com.chamcham.backend.entity;

import com.chamcham.backend.entity.enums.DealType;
import com.chamcham.backend.entity.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
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
    private Package aPackage;

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

    /** SAR amount in integer halala (nullable for barter-only deals). */
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
    private LocalDate deadlineDate;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Deliverable> deliverables = new ArrayList<>();
}
