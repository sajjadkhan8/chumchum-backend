package com.zingzing.backend.repository;

import com.zingzing.backend.entity.SafepayPaymentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SafepayPaymentSessionRepository extends JpaRepository<SafepayPaymentSession, UUID> {

    Optional<SafepayPaymentSession> findByTrackerToken(String trackerToken);

    Optional<SafepayPaymentSession> findByIdAndBrandId(UUID id, UUID brandId);

    List<SafepayPaymentSession> findByBrandIdOrderByCreatedAtDesc(UUID brandId);

    /** Mark stale INITIATED sessions as EXPIRED. Called by a scheduled job. */
    @Modifying
    @Query("""
        UPDATE SafepayPaymentSession s
           SET s.status = 'EXPIRED'
         WHERE s.status = 'INITIATED'
           AND s.expiresAt < :now
        """)
    int expireOldSessions(@Param("now") Instant now);

    boolean existsByTrackerToken(String trackerToken);
}
