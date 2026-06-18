package com.zingzing.backend.storage;

import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;

/**
 * Pluggable file-storage back-end.
 * Current implementation: local disk. Future: S3.
 */
public interface StorageStrategy {
    /**
     * Persist {@code file} under {@code subfolder} and return the PUBLIC URL for the stored object.
     */
    String store(MultipartFile file, String filename, String subfolder);

    /**
     * Return a URL-path (e.g. /api/v1/files/subfolder/name) for protected (auth-required) access.
     */
    String protectedPath(String filename, String subfolder);

    /**
     * Resolve a relative path to a local Path for streaming (local only; S3 must override).
     */
    Path load(String relativePath);
}
