package com.zingzing.backend.controller;

import com.zingzing.backend.config.security.AuthenticatedUser;
import com.zingzing.backend.config.MediaUploadProperties;
import com.zingzing.backend.dto.media.MediaUploadLimitsResponse;
import com.zingzing.backend.dto.media.MediaUploadResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

import com.zingzing.backend.exception.ApiException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/uploads")
public class UploadController {

    private final FileStorageService fileStorageService;
    private final MediaUploadProperties mediaUploadProperties;
    private final OrderRepository orderRepository;
    private final DeliverableRepository deliverableRepository;

    public UploadController(FileStorageService fileStorageService, MediaUploadProperties mediaUploadProperties, OrderRepository orderRepository,
                            DeliverableRepository deliverableRepository) {
        this.fileStorageService = fileStorageService;
        this.mediaUploadProperties = mediaUploadProperties;
        this.orderRepository = orderRepository;
        this.deliverableRepository = deliverableRepository;
    }

    @GetMapping("/limits")
    public ResponseEntity<Map<String, Object>> limits() {
        Map<String, MediaUploadLimitsResponse.UploadRuleResponse> rules = mediaUploadProperties.getUploads().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, entry -> {
                    var rule = entry.getValue();
                    return new MediaUploadLimitsResponse.UploadRuleResponse(rule.maxMb(), rule.allowedTypes(), rule.resourceType());
                }));
        return ResponseEntity.ok(Map.of("success", true, "data", new MediaUploadLimitsResponse(
                mediaUploadProperties.getUserStorageLimitMb(),
                mediaUploadProperties.getPackageStorageLimitMb(),
                mediaUploadProperties.getCampaignStorageLimitMb(),
                mediaUploadProperties.getUserUploadCountLimit(),
                mediaUploadProperties.getPackageUploadCountLimit(),
                mediaUploadProperties.getCampaignUploadCountLimit(),
                rules
        )));
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> avatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        requireCreator(authUser);
        return ok(fileStorageService.validateStoreAndRecord(file, authUser.userId(), "avatar", "avatars/" + authUser.userId(), null, null, false));
    }

    @PostMapping(value = "/cover-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> coverImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        requireCreator(authUser);
        return ok(fileStorageService.validateStoreAndRecord(file, authUser.userId(), "cover-image", "covers/" + authUser.userId(), null, null, false));
    }

    @PostMapping(value = "/content-preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> contentPreview(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) UUID packageId,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        requireCreator(authUser);
        String folder = packageId == null ? "previews/" + authUser.userId() : "packages/" + packageId + "/previews";
        return ok(fileStorageService.validateStoreAndRecord(file, authUser.userId(), "content-preview", folder, packageId, packageId == null ? null : "package", false));
    }

    @PostMapping(value = "/package-thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> packageThumbnail(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) UUID packageId,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        String folder = packageId == null ? "packages/" + authUser.userId() + "/thumbnails" : "packages/" + packageId;
        return ok(fileStorageService.validateStoreAndRecord(file, authUser.userId(), "package-thumbnail", folder, packageId, packageId == null ? null : "package", false));
    }

    @PostMapping(value = "/campaign-cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> campaignCover(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) UUID campaignId,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        if (!authUser.role().isBrand() && !authUser.role().isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only brands can upload campaign media");
        }
        String folder = campaignId == null ? "campaigns/" + authUser.userId() + "/covers" : "campaigns/" + campaignId;
        return ok(fileStorageService.validateStoreAndRecord(file, authUser.userId(), "campaign-cover", folder, campaignId, campaignId == null ? null : "campaign", false));
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
        return ok(fileStorageService.validateStoreAndRecord(file, authUser.userId(), "deliverable", folder, deliverableId, "deliverable", true));
    }

    @PostMapping(value = "/brand-logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> brandLogo(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        if (!authUser.role().isBrand() && !authUser.role().isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only brands can upload brand logos");
        }
        return ok(fileStorageService.validateStoreAndRecord(file, authUser.userId(), "brand-logo", "brands/" + authUser.userId(), null, null, false));
    }

    @PostMapping(value = "/verification-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> verificationDocument(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser authUser) {
        if (!authUser.role().isCreator() && !authUser.role().isBrand() && !authUser.role().isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only account owners can upload verification documents");
        }
        return ok(fileStorageService.validateStoreAndRecord(file, authUser.userId(), "verification-document", "verification/" + authUser.userId(), null, null, true));
    }

    private void requireCreator(AuthenticatedUser authUser) {
        if (!authUser.role().isCreator() && !authUser.role().isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only creators can upload this media");
        }
    }

    private ResponseEntity<Map<String, Object>> ok(MediaUploadResponse response) {
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }
}
