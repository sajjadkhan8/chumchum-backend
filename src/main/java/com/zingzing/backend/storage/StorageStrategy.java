package com.zingzing.backend.storage;

import com.zingzing.backend.dto.media.StoredMedia;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;

/**
 * Pluggable file-storage back-end.
 * Current implementation: local disk. Future: S3.
 */
public interface StorageStrategy {
    /**
     * Persist {@code file} under {@code subfolder} and return the stored object metadata.
     */
    StoredMedia store(MultipartFile file, String filename, String subfolder, String resourceType);

    /**
     * Return a URL-path (e.g. /api/v1/files/subfolder/name) for protected (auth-required) access.
     */
    String protectedPath(String filename, String subfolder);

    /**
     * Resolve a relative path to a local Path for streaming (local only; S3 must override).
     */
    Path load(String relativePath);
}
