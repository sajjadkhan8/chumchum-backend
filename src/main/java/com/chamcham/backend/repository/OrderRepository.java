package com.chamcham.backend.repository;

import com.chamcham.backend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query("select o from Order o where o.completed = true and (o.seller.id = :userId or o.buyer.id = :userId)")
    List<Order> findCompletedByParticipant(UUID userId);

    Optional<Order> findByPaymentIntent(String paymentIntent);
}

