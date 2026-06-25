package com.zingzing.backend.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.zingzing.backend.config.MediaUploadProperties;
import com.zingzing.backend.dto.media.StoredMedia;
import com.zingzing.backend.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.storage.driver", havingValue = "cloudinary")
public class CloudinaryStorageStrategy implements StorageStrategy {

    private static final long CLOUDINARY_SINGLE_UPLOAD_LIMIT_BYTES = 100L * 1024L * 1024L;
    private static final int CLOUDINARY_CHUNK_SIZE_BYTES = 20 * 1024 * 1024;

    private final Cloudinary cloudinary;
    private final MediaUploadProperties properties;

    public CloudinaryStorageStrategy(@Value("${cloudinary.url}") String cloudinaryUrl,
                                     MediaUploadProperties properties) {
        this.cloudinary = new Cloudinary(cloudinaryUrl);
        this.cloudinary.config.secure = true;
        this.properties = properties;
    }

    @Override
    public StoredMedia store(MultipartFile file, String filename, String subfolder, String resourceType) {
        try {
            String resolvedResourceType = "auto".equals(resourceType) ? resolveResourceType(file.getContentType()) : resourceType;
            String folder = safeFolder(properties.getRootFolder()) + "/" + safeFolder(subfolder);
            Map<String, Object> options = ObjectUtils.asMap(
                    "folder", folder,
                    "public_id", filenameWithoutExtension(filename),
                    "resource_type", resolvedResourceType,
                    "overwrite", false,
                    "use_filename", false,
                    "unique_filename", true,
                    "quality_analysis", false,
                    "type", "upload"
            );
            if ("image".equals(resolvedResourceType)) {
                options.put("transformation", "q_auto:good,f_auto");
                options.put("eager", "c_fill,g_auto,w_1200,h_1200/q_auto:good/f_auto");
            } else if ("video".equals(resolvedResourceType)) {
                options.put("eager", "q_auto:good/f_auto");
                options.put("eager_async", true);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> response = file.getSize() > CLOUDINARY_SINGLE_UPLOAD_LIMIT_BYTES
                    ? cloudinary.uploader().uploadLarge(file.getInputStream(), options, CLOUDINARY_CHUNK_SIZE_BYTES)
                    : cloudinary.uploader().upload(file.getInputStream(), options);
            return toStoredMedia(response, resolvedResourceType);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read uploaded file");
        } catch (RuntimeException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Cloudinary upload failed");
        }
    }

    @Override
    public String protectedPath(String filename, String subfolder) {
        return "/api/v1/files/" + subfolder + "/" + filename;
    }

    @Override
    public Path load(String relativePath) {
        throw new UnsupportedOperationException("Cloudinary assets are not available as local paths");
    }

    private StoredMedia toStoredMedia(Map<String, Object> response, String fallbackResourceType) {
        String secureUrl = asString(response.get("secure_url"));
        String publicId = asString(response.get("public_id"));
        String resourceType = valueOr(asString(response.get("resource_type")), fallbackResourceType);
        String format = asString(response.get("format"));
        String thumbnailUrl = "image".equals(resourceType) ? optimizedImageUrl(secureUrl) : videoThumbnailUrl(secureUrl, format);
        return new StoredMedia(
                secureUrl,
                secureUrl,
                thumbnailUrl,
                publicId,
                asString(response.get("asset_id")),
                resourceType,
                format,
                asLong(response.get("bytes")),
                asInteger(response.get("width")),
                asInteger(response.get("height")),
                asDouble(response.get("duration"))
        );
    }

    private String optimizedImageUrl(String secureUrl) {
        if (secureUrl == null) return null;
        return secureUrl.replace("/upload/", "/upload/c_fill,g_auto,w_600,h_600,q_auto:good,f_auto/");
    }

    private String videoThumbnailUrl(String secureUrl, String format) {
        if (secureUrl == null) return null;
        String noFormat = format == null ? secureUrl : secureUrl.replaceAll("\\." + format + "$", ".jpg");
        return noFormat.replace("/video/upload/", "/video/upload/so_0,c_fill,g_auto,w_600,h_600,q_auto:good,f_jpg/");
    }

    private String resolveResourceType(String contentType) {
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (type.startsWith("image/")) return "image";
        if (type.startsWith("video/")) return "video";
        return "raw";
    }

    private String filenameWithoutExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private String safeFolder(String value) {
        return value == null ? "" : value.replaceAll("[^a-zA-Z0-9/_-]", "-").replaceAll("/+", "/").replaceAll("^/|/$", "");
    }

    private String asString(Object value) { return value == null ? null : String.valueOf(value); }
    private String valueOr(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private Long asLong(Object value) { return value instanceof Number number ? number.longValue() : null; }
    private Integer asInteger(Object value) { return value instanceof Number number ? number.intValue() : null; }
    private Double asDouble(Object value) { return value instanceof Number number ? number.doubleValue() : null; }
}
