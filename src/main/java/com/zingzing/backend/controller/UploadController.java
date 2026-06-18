package com.zingzing.backend.controller;

import com.zingzing.backend.config.security.AuthenticatedUser;
import com.zingzing.backend.service.FileStorageService;
import com.zingzing.backend.entity.Deliverable;
import com.zingzing.backend.entity.Order;
import com.zingzing.backend.entity.enums.OrderStatus;
import com.zingzing.backend.repository.DeliverableRepository;
import com.zingzing.backend.repository.OrderRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.zingzing.backend.service.FileStorageService.IMAGE_TYPES;
import static com.zingzing.backend.service.FileStorageService.PRIVATE_FILE_TYPES;
import static com.zingzing.backend.service.FileStorageService.VIDEO_TYPES;
import com.zingzing.backend.exception.ApiException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/uploads")
public class UploadController {

    private final FileStorageService fileStorageService;
    private final OrderRepository orderRepository;
    private final DeliverableRepository deliverableRepository;

    public UploadController(FileStorageService fileStorageService, OrderRepository orderRepository,
                            DeliverableRepository deliverableRepository) {
        this.fileStorageService = fileStorageService;
        this.orderRepository = orderRepository;
        this.deliverableRepository = deliverableRepository;
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> avatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        requireCreator(authUser);
        return ok(fileStorageService.validateAndStore(file, IMAGE_TYPES, 5, "avatars"));
    }

    @PostMapping(value = "/cover-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> coverImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        requireCreator(authUser);
        return ok(fileStorageService.validateAndStore(file, IMAGE_TYPES, 10, "covers"));
    }

    @PostMapping(value = "/content-preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> contentPreview(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String platform,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        requireCreator(authUser);
        Set<String> allowed = new HashSet<>(IMAGE_TYPES);
        allowed.addAll(VIDEO_TYPES);
        return ok(fileStorageService.validateAndStore(file, allowed, 100, "previews"));
    }

    @PostMapping(value = "/package-thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> packageThumbnail(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        return ok(fileStorageService.validateAndStore(file, IMAGE_TYPES, 5, "packages"));
    }

    @PostMapping(value = "/deliverable", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> deliverable(
            @RequestParam("file") MultipartFile file,
            @RequestParam UUID orderId,
            @RequestParam UUID deliverableId,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        requireCreator(authUser);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getCreator().getId().equals(authUser.userId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the order creator can upload deliverables");
        }
        if (order.getStatus() != OrderStatus.IN_PROGRESS && order.getStatus() != OrderStatus.REVISION) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Deliverables can only be uploaded while work is in progress or revision");
        }
        Deliverable deliverable = deliverableRepository.findById(deliverableId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Deliverable not found"));
        if (!deliverable.getOrder().getId().equals(orderId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Deliverable does not belong to this order");
        }
        String folder = "deliverables/" + orderId + "/" + deliverableId;
        return ok(fileStorageService.validateAndStoreProtected(file, PRIVATE_FILE_TYPES, 500, folder));
    }

    @PostMapping(value = "/brand-logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> brandLogo(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        if (!authUser.role().isBrand() && !authUser.role().isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only brands can upload brand logos");
        }
        return ok(fileStorageService.validateAndStore(file, IMAGE_TYPES, 5, "brands"));
    }

    private void requireCreator(AuthenticatedUser authUser) {
        if (!authUser.role().isCreator() && !authUser.role().isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only creators can upload this media");
        }
    }

    private ResponseEntity<Map<String, Object>> ok(String url) {
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("url", url)));
    }
}
