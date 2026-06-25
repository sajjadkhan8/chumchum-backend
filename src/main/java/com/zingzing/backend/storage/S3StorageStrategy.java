package com.zingzing.backend.storage;

import com.zingzing.backend.dto.media.StoredMedia;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;

/**
 * S3-backed storage strategy.
 * Activated by setting STORAGE_DRIVER=s3 (app.storage.driver=s3).
 * Requires AWS_BUCKET, AWS_REGION, AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY env vars.
 * Full implementation pending — add aws-java-sdk-s3 or software.amazon.awssdk:s3 dependency when wiring.
 */
@Component
@ConditionalOnProperty(name = "app.storage.driver", havingValue = "s3")
public class S3StorageStrategy implements StorageStrategy {

    @Override
    public StoredMedia store(MultipartFile file, String filename, String subfolder, String resourceType) {
        throw new UnsupportedOperationException("S3 storage not yet implemented. Set STORAGE_DRIVER=local.");
    }

    @Override
    public String protectedPath(String filename, String subfolder) {
        throw new UnsupportedOperationException("S3 storage not yet implemented.");
    }

    @Override
    public Path load(String relativePath) {
        throw new UnsupportedOperationException("S3 does not support local Path streaming. Use pre-signed URLs.");
    }
}
