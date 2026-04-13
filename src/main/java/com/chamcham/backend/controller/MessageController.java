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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public ResponseEntity<MessageResponse> create(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody MessageCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messageService.createMessage(authUser.userId(), authUser.seller(), request));
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<List<MessageResponse>> list(@PathVariable UUID conversationId) {
        return ResponseEntity.ok(messageService.getMessages(conversationId));
    }
}

