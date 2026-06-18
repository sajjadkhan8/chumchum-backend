package com.zingzing.backend.controller;

import com.zingzing.backend.config.security.AuthenticatedUser;
import com.zingzing.backend.dto.message.MessageCreateRequest;
import com.zingzing.backend.dto.message.MessageResponse;
import com.zingzing.backend.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @PostMapping(value = "/messages/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponse> sendAttachment(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messageService.sendAttachmentMessage(authUser.userId(), authUser.role(), conversationId, file));
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

