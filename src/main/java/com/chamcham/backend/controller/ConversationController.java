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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public ResponseEntity<List<ConversationResponse>> getConversations(@AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.ok(conversationService.getConversations(authUser.userId(), authUser.seller()));
    }

    @PostMapping
    public ResponseEntity<ConversationResponse> createConversation(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody ConversationCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(conversationService.createConversation(authUser.userId(), authUser.seller(), request));
    }

    @GetMapping("/single/{sellerId}/{buyerId}")
    public ResponseEntity<ConversationResponse> getSingle(
            @PathVariable UUID sellerId,
            @PathVariable UUID buyerId
    ) {
        return ResponseEntity.ok(conversationService.getSingleConversation(sellerId, buyerId));
    }

    @PatchMapping("/{conversationId}")
    public ResponseEntity<ConversationResponse> updateConversation(@PathVariable UUID conversationId) {
        return ResponseEntity.ok(conversationService.markAsRead(conversationId));
    }
}

