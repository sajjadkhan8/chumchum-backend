package com.zingzing.backend.controller;

import com.zingzing.backend.config.security.AuthenticatedUser;
import com.zingzing.backend.dto.creator.CreatorResponse;
import com.zingzing.backend.mapper.CreatorMapper;
import com.zingzing.backend.service.SavedCreatorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/saved-creators")
public class SavedCreatorController {

    private final SavedCreatorService savedCreatorService;
    private final CreatorMapper creatorMapper;

    public SavedCreatorController(SavedCreatorService savedCreatorService, CreatorMapper creatorMapper) {
        this.savedCreatorService = savedCreatorService;
        this.creatorMapper = creatorMapper;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(@AuthenticationPrincipal AuthenticatedUser authUser) {
        List<CreatorResponse> creators = savedCreatorService.getSaved(authUser.userId(), authUser.role())
                .stream().map(creatorMapper::toResponse).toList();
        return ResponseEntity.ok(Map.of("success", true, "data",
                Map.of("creators", creators, "total", creators.size())));
    }

    @PostMapping("/{creatorId}")
    public ResponseEntity<Map<String, Object>> save(
            @PathVariable UUID creatorId,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        savedCreatorService.save(authUser.userId(), creatorId, authUser.role());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("success", true, "message", "Creator saved"));
    }

    @DeleteMapping("/{creatorId}")
    public ResponseEntity<Map<String, Object>> unsave(
            @PathVariable UUID creatorId,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        savedCreatorService.unsave(authUser.userId(), creatorId, authUser.role());
        return ResponseEntity.ok(Map.of("success", true, "message", "Creator removed from saved list"));
    }
}

