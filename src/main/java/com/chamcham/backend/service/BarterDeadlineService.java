package com.chamcham.backend.service;

import com.chamcham.backend.entity.Order;
import com.chamcham.backend.entity.enums.DealType;
import com.chamcham.backend.entity.enums.OrderStatus;
import com.chamcham.backend.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class BarterDeadlineService {

    private static final Logger log = LoggerFactory.getLogger(BarterDeadlineService.class);

    private static final Set<OrderStatus> ACTIVE_STATUSES = EnumSet.of(
            OrderStatus.ACCEPTED, OrderStatus.IN_PROGRESS,
            OrderStatus.DELIVERED, OrderStatus.REVIEW, OrderStatus.REVISION);

    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    public BarterDeadlineService(OrderRepository orderRepository, NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void checkBarterDeadlines() {
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Karachi"));
        List<Order> overdueOrders = orderRepository.findOverdueBarterOrders(now);

        log.info("Barter deadline check: {} overdue orders", overdueOrders.size());

        for (Order order : overdueOrders) {
            String body = "Barter product for order " + order.getOrderNumber() + " was expected by "
                    + order.getBarterExpectedBy().atZoneSameInstant(ZoneId.of("Asia/Karachi")).toLocalDate()
                    + ". Please confirm receipt or contact the brand to resolve.";

            notificationService.send(order.getCreator().getId(), "barter_deadline",
                    "Barter product deadline passed", body, "order", order.getId());
            notificationService.send(order.getBrand().getId(), "barter_deadline",
                    "Barter deadline passed", "Order " + order.getOrderNumber()
                            + ": creator has not yet confirmed receipt of barter product.", "order", order.getId());
        }
    }
}
