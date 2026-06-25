package com.zingzing.backend.service;

import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.config.MediaUploadProperties;
import com.zingzing.backend.dto.media.MediaUploadResponse;
import com.zingzing.backend.dto.media.StoredMedia;
import com.zingzing.backend.entity.MediaAsset;
import com.zingzing.backend.entity.User;
import com.zingzing.backend.repository.MediaAssetRepository;
import com.zingzing.backend.repository.UserRepository;
import com.zingzing.backend.storage.CloudinaryStorageStrategy;
import com.zingzing.backend.storage.StorageStrategy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    public static final Set<String> IMAGE_TYPES  = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    public static final Set<String> VIDEO_TYPES  = Set.of("video/mp4", "video/quicktime", "video/x-msvideo");
    public static final Set<String> DOCUMENT_TYPES = Set.of(
            "application/pdf", "text/plain", "application/zip",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    public static final Set<String> PRIVATE_FILE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif",
            "video/mp4", "video/quicktime", "video/x-msvideo",
            "application/pdf", "text/plain", "application/zip",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    public static final long MB = 1024L * 1024L;
    private static final Map<String, Set<String>> TYPE_EXTENSIONS = Map.ofEntries(
            Map.entry("image/jpeg", Set.of(".jpg", ".jpeg")),
            Map.entry("image/png", Set.of(".png")),
            Map.entry("image/webp", Set.of(".webp")),
            Map.entry("image/gif", Set.of(".gif")),
            Map.entry("video/mp4", Set.of(".mp4")),
            Map.entry("video/quicktime", Set.of(".mov")),
            Map.entry("video/x-msvideo", Set.of(".avi")),
            Map.entry("application/pdf", Set.of(".pdf")),
            Map.entry("text/plain", Set.of(".txt")),
            Map.entry("application/zip", Set.of(".zip")),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", Set.of(".docx")),
            Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", Set.of(".xlsx"))
    );

    private final StorageStrategy storageStrategy;
    private final MediaUploadProperties properties;
    private final MediaAssetRepository mediaAssetRepository;
    private final UserRepository userRepository;

    public FileStorageService(StorageStrategy storageStrategy,
                              MediaUploadProperties properties,
                              MediaAssetRepository mediaAssetRepository,
                              UserRepository userRepository) {
        this.storageStrategy = storageStrategy;
        this.properties = properties;
        this.mediaAssetRepository = mediaAssetRepository;
        this.userRepository = userRepository;
    }

    /**
     * Validate content-type and size, then store the file under {@code subfolder}.
     *
     * @return the public URL of the stored file
     */
    public MediaUploadResponse validateAndStore(MultipartFile file, Set<String> allowedTypes, long maxMb, String subfolder) {
        validate(file, allowedTypes, maxMb);
        String filename = generateFilename(file);
        StoredMedia stored = storageStrategy.store(file, filename, subfolder, resourceType(file.getContentType()));
        return new MediaUploadResponse(null, stored.url(), stored.secureUrl(), stored.thumbnailUrl(), stored.publicId(),
                stored.resourceType(), stored.format(), stored.bytes(), stored.width(), stored.height(), stored.duration());
    }

    public MediaUploadResponse validateAndStoreProtected(MultipartFile file, Set<String> allowedTypes, long maxMb, String subfolder) {
        validate(file, allowedTypes, maxMb);
        String filename = generateFilename(file);
        StoredMedia stored = storageStrategy.store(file, filename, subfolder, resourceType(file.getContentType()));
        String protectedPath = storageStrategy.protectedPath(filename, subfolder);
        String url = protectedPath == null ? stored.secureUrl() : protectedPath;
        return new MediaUploadResponse(null, url, stored.secureUrl(), stored.thumbnailUrl(), stored.publicId(),
                stored.resourceType(), stored.format(), stored.bytes(), stored.width(), stored.height(), stored.duration());
    }

    @Transactional
    public MediaUploadResponse validateStoreAndRecord(MultipartFile file, UUID ownerId, String kind, String folder,
                                                      UUID entityId, String entityType, boolean protectedAccess) {
        MediaUploadProperties.UploadRule rule = properties.rule(kind);
        Set<String> allowedTypes = Set.copyOf(rule.allowedTypes());
        validate(file, allowedTypes, rule.maxMb());
        enforceUserQuota(ownerId, file.getSize());
        enforceEntityQuota(ownerId, entityType, entityId, file.getSize());

        String filename = generateFilename(file);
        StoredMedia stored = storageStrategy.store(file, filename, folder, rule.resourceType());
        String appPath = protectedAccess ? storageStrategy.protectedPath(filename, folder) : null;
        String url = appPath == null ? stored.secureUrl() : appPath;
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        MediaAsset asset = new MediaAsset();
        asset.setOwner(owner);
        asset.setScope(kind);
        asset.setEntityId(entityId);
        asset.setEntityType(entityType);
        asset.setPublicId(stored.publicId() == null ? folder + "/" + filename : stored.publicId());
        asset.setAssetId(stored.assetId());
        asset.setResourceType(stored.resourceType() == null ? resourceType(file.getContentType()) : stored.resourceType());
        asset.setFormat(stored.format());
        asset.setSecureUrl(stored.secureUrl() == null ? url : stored.secureUrl());
        asset.setAppPath(appPath);
        asset.setThumbnailUrl(stored.thumbnailUrl());
        asset.setOriginalFilename(sanitizeOriginalFilename(file.getOriginalFilename()));
        asset.setContentType(file.getContentType());
        asset.setBytes(stored.bytes() == null ? file.getSize() : stored.bytes());
        asset.setWidth(stored.width());
        asset.setHeight(stored.height());
        asset.setDuration(stored.duration());
        asset = mediaAssetRepository.save(asset);

        return new MediaUploadResponse(asset.getId(), url, protectedAccess ? null : asset.getSecureUrl(), asset.getThumbnailUrl(),
                asset.getPublicId(), asset.getResourceType(), asset.getFormat(), asset.getBytes(),
                asset.getWidth(), asset.getHeight(), asset.getDuration());
    }

    private void validate(MultipartFile file, Set<String> allowedTypes, long maxMb) {
        if (file.isEmpty())
            throw new ApiException(HttpStatus.BAD_REQUEST, "File must not be empty");
        String ct = file.getContentType();
        String normalizedType = ct == null ? null : ct.toLowerCase(Locale.ROOT);
        if (normalizedType == null || !allowedTypes.contains(normalizedType))
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Unsupported file type: " + ct + ". Allowed: " + allowedTypes);
        if (file.getSize() > maxMb * MB)
            throw new ApiException(HttpStatus.BAD_REQUEST, "File exceeds maximum size of " + maxMb + " MB");
        validateExtension(file.getOriginalFilename(), normalizedType);
        validateSignature(file, normalizedType);
    }

    /**
     * Store without type validation (caller is responsible for validation).
     *
     * @return the public URL of the stored file
     */
    public String store(MultipartFile file, String subfolder) {
        String filename = generateFilename(file);
        return storageStrategy.store(file, filename, subfolder, resourceType(file.getContentType())).url();
    }

    public Path load(String relativePath) {
        return storageStrategy.load(relativePath);
    }

    public Optional<String> remoteUrlForProtectedPath(String appPath) {
        if (!(storageStrategy instanceof CloudinaryStorageStrategy)) {
            return Optional.empty();
        }
        return mediaAssetRepository.findByAppPathAndDeletedFalse(appPath)
                .map(MediaAsset::getSecureUrl)
                .filter(url -> url != null && !url.isBlank());
    }

    private String generateFilename(MultipartFile file) {
        String ext = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains("."))
            ext = original.substring(original.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        return UUID.randomUUID() + ext;
    }

    private void enforceUserQuota(UUID ownerId, long newBytes) {
        if (mediaAssetRepository.countByOwner_IdAndDeletedFalse(ownerId) >= properties.getUserUploadCountLimit()) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "User upload count limit reached");
        }
        long limitBytes = properties.getUserStorageLimitMb() * MB;
        if (mediaAssetRepository.sumBytesByOwner(ownerId) + newBytes > limitBytes) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "User storage limit reached");
        }
    }

    private void enforceEntityQuota(UUID ownerId, String entityType, UUID entityId, long newBytes) {
        if (entityType == null || entityId == null) return;
        int countLimit = "campaign".equals(entityType) ? properties.getCampaignUploadCountLimit() : properties.getPackageUploadCountLimit();
        long storageLimitMb = "campaign".equals(entityType) ? properties.getCampaignStorageLimitMb() : properties.getPackageStorageLimitMb();
        if (mediaAssetRepository.countByOwner_IdAndEntityTypeAndEntityIdAndDeletedFalse(ownerId, entityType, entityId) >= countLimit) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, entityType + " upload count limit reached");
        }
        if (mediaAssetRepository.sumBytesByOwnerAndEntity(ownerId, entityType, entityId) + newBytes > storageLimitMb * MB) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, entityType + " storage limit reached");
        }
    }

    private String resourceType(String contentType) {
        String normalized = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("image/")) return "image";
        if (normalized.startsWith("video/")) return "video";
        return "raw";
    }

    private String sanitizeOriginalFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) return "upload";
        String name = originalFilename.replace("\\", "/");
        name = name.substring(name.lastIndexOf('/') + 1);
        return name.replaceAll("[\\r\\n\\t]", "_");
    }

    private void validateExtension(String originalFilename, String contentType) {
        String name = originalFilename == null ? "" : originalFilename.toLowerCase(Locale.ROOT);
        Set<String> extensions = TYPE_EXTENSIONS.get(contentType);
        if (extensions == null || extensions.stream().noneMatch(name::endsWith)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "File extension does not match its content type");
        }
    }

    private void validateSignature(MultipartFile file, String contentType) {
        try (var input = file.getInputStream()) {
            byte[] header = input.readNBytes(16);
            boolean valid = switch (contentType) {
                case "image/jpeg" -> startsWith(header, 0xFF, 0xD8, 0xFF);
                case "image/png" -> startsWith(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
                case "image/gif" -> asciiAt(header, 0, "GIF8");
                case "image/webp" -> asciiAt(header, 0, "RIFF") && asciiAt(header, 8, "WEBP");
                case "application/pdf" -> asciiAt(header, 0, "%PDF");
                case "application/zip",
                     "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                     "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> startsWith(header, 0x50, 0x4B);
                case "video/mp4", "video/quicktime" -> asciiAt(header, 4, "ftyp");
                case "video/x-msvideo" -> asciiAt(header, 0, "RIFF") && asciiAt(header, 8, "AVI ");
                case "text/plain" -> {
                    byte[] sample = new byte[4096];
                    int read = input.read(sample);
                    boolean containsNull = containsNull(header, header.length)
                            || containsNull(sample, Math.max(read, 0));
                    yield !containsNull;
                }
                default -> false;
            };
            if (!valid) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "File content does not match its declared type");
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not inspect uploaded file");
        }
    }

    private boolean startsWith(byte[] bytes, int... expected) {
        if (bytes.length < expected.length) return false;
        for (int index = 0; index < expected.length; index++) {
            if ((bytes[index] & 0xFF) != expected[index]) return false;
        }
        return true;
    }

    private boolean asciiAt(byte[] bytes, int offset, String value) {
        if (bytes.length < offset + value.length()) return false;
        for (int index = 0; index < value.length(); index++) {
            if (bytes[offset + index] != (byte) value.charAt(index)) return false;
        }
        return true;
    }

    private boolean containsNull(byte[] bytes, int length) {
        for (int index = 0; index < length; index++) {
            if (bytes[index] == 0) return true;
        }
        return false;
    }
}
