package com.zingzing.backend.controller;

import com.zingzing.backend.config.security.AuthenticatedUser;
import com.zingzing.backend.entity.Deliverable;
import com.zingzing.backend.entity.Message;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.repository.DeliverableRepository;
import com.zingzing.backend.repository.MessageRepository;
import com.zingzing.backend.service.FileStorageService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
public class ProtectedFileController {

    private final FileStorageService fileStorageService;
    private final DeliverableRepository deliverableRepository;
    private final MessageRepository messageRepository;

    public ProtectedFileController(FileStorageService fileStorageService, DeliverableRepository deliverableRepository,
                                   MessageRepository messageRepository) {
        this.fileStorageService = fileStorageService;
        this.deliverableRepository = deliverableRepository;
        this.messageRepository = messageRepository;
    }

    @GetMapping("/deliverables/{orderId}/{deliverableId}/{filename}")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> deliverable(@PathVariable UUID orderId, @PathVariable UUID deliverableId,
                                                @PathVariable String filename,
                                                @AuthenticationPrincipal AuthenticatedUser authUser) {
        String url = "/api/v1/files/deliverables/" + orderId + "/" + deliverableId + "/" + filename;
        Deliverable deliverable = deliverableRepository.findById(deliverableId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Deliverable not found"));
        if (!deliverable.getOrder().getId().equals(orderId) || !url.equals(deliverable.getFileUrl())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "File not found");
        }
        if (!authUser.role().isAdmin()
                && !deliverable.getOrder().getCreator().getId().equals(authUser.userId())
                && !deliverable.getOrder().getBrand().getId().equals(authUser.userId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return download("deliverables/" + orderId + "/" + deliverableId + "/" + filename);
    }

    @GetMapping("/attachments/{conversationId}/{filename}")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> attachment(@PathVariable UUID conversationId, @PathVariable String filename,
                                               @AuthenticationPrincipal AuthenticatedUser authUser) {
        String url = "/api/v1/files/attachments/" + conversationId + "/" + filename;
        Message message = messageRepository.findByAttachmentUrl(url)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "File not found"));
        if (!message.getConversation().getId().equals(conversationId)
                || (!message.getConversation().getCreator().getId().equals(authUser.userId())
                && !message.getConversation().getBrand().getId().equals(authUser.userId())
                && !authUser.role().isAdmin())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return download("attachments/" + conversationId + "/" + filename, message.getAttachmentOriginalName());
    }

    private ResponseEntity<Resource> download(String relativePath) {
        return download(relativePath, null);
    }

    private ResponseEntity<Resource> download(String relativePath, String downloadName) {
        Path path = fileStorageService.load(relativePath);
        String contentType;
        try {
            contentType = Files.probeContentType(path);
        } catch (IOException exception) {
            contentType = null;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(downloadName == null || downloadName.isBlank() ? path.getFileName().toString() : downloadName)
                .build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType))
                .body(new FileSystemResource(path));
    }
}
