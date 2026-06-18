package com.chamcham.backend.controller;

import com.chamcham.backend.config.security.AuthenticatedUser;
import com.chamcham.backend.dto.conversation.ConversationCreateRequest;
import com.chamcham.backend.dto.conversation.ConversationResponse;
import com.chamcham.backend.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getConversations(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(conversationService.getConversations(authUser.userId(), authUser.role(), page, limit));
    }

    @PostMapping
    public ResponseEntity<ConversationResponse> createConversation(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody ConversationCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(conversationService.createConversation(authUser.userId(), authUser.role(), request));
    }

    @GetMapping("/single/{creatorId}/{brandId}")
    public ResponseEntity<ConversationResponse> getSingle(
            @PathVariable UUID creatorId,
            @PathVariable UUID brandId,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        return ResponseEntity.ok(conversationService.getSingleConversation(
                creatorId, brandId, authUser.userId(), authUser.role()));
    }
}
