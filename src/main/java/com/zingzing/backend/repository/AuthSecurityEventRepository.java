package com.zingzing.backend.repository;

import com.zingzing.backend.entity.AuthSecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuthSecurityEventRepository extends JpaRepository<AuthSecurityEvent, UUID> {
}
