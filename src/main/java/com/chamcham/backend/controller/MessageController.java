package com.chamcham.backend.controller;

import com.chamcham.backend.config.security.AuthenticatedUser;
import com.chamcham.backend.dto.message.MessageCreateRequest;
import com.chamcham.backend.dto.message.MessageResponse;
import com.chamcham.backend.service.MessageService;
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
@RequestMapping("/api/v1/conversations/{conversationId}")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/messages")
    public ResponseEntity<MessageResponse> sendText(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody MessageCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messageService.sendTextMessage(authUser.userId(), authUser.role(), conversationId, request));
    }

    @PostMapping("/messages/offer")
    public ResponseEntity<MessageResponse> sendOffer(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody MessageCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messageService.sendOfferMessage(authUser.userId(), authUser.role(), conversationId, request));
    }

    @GetMapping("/messages")
    public ResponseEntity<List<MessageResponse>> list(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        return ResponseEntity.ok(messageService.getMessages(conversationId, authUser.userId()));
    }

    @PatchMapping("/read")
    public ResponseEntity<Void> markRead(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        messageService.markRead(conversationId, authUser.userId(), authUser.role());
        return ResponseEntity.noContent().build();
    }
}

