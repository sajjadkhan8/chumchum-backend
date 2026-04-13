package com.chamcham.backend.controller;

import com.chamcham.backend.config.security.AuthenticatedUser;
import com.chamcham.backend.dto.creator.CreatorCreateRequest;
import com.chamcham.backend.dto.creator.CreatorResponse;
import com.chamcham.backend.dto.creator.CreatorUpdateRequest;
import com.chamcham.backend.service.CreatorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/creators")
public class CreatorController {

    private final CreatorService creatorService;

    public CreatorController(CreatorService creatorService) {
        this.creatorService = creatorService;
    }

    @PostMapping
    public ResponseEntity<CreatorResponse> create(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody CreatorCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(creatorService.create(authUser.userId(), authUser.role(), request));
    }

    @GetMapping
    public ResponseEntity<List<CreatorResponse>> getAll() {
        return ResponseEntity.ok(creatorService.getAll());
    }

    @GetMapping("/{creatorId}")
    public ResponseEntity<CreatorResponse> getById(@PathVariable UUID creatorId) {
        return ResponseEntity.ok(creatorService.getById(creatorId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<CreatorResponse> getByUserId(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(creatorService.getByUserId(authUser.userId(), authUser.role(), userId));
    }

    @PutMapping("/{creatorId}")
    public ResponseEntity<CreatorResponse> update(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable UUID creatorId,
            @Valid @RequestBody CreatorUpdateRequest request
    ) {
        return ResponseEntity.ok(creatorService.update(authUser.userId(), authUser.role(), creatorId, request));
    }
}

