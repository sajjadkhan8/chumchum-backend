package com.zingzing.backend.repository;

import com.zingzing.backend.entity.Deliverable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliverableRepository extends JpaRepository<Deliverable, UUID> {
    List<Deliverable> findByOrderId(UUID orderId);
}

