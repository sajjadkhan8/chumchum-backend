package com.chamcham.backend.service;

import com.chamcham.backend.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

    private final Path uploadRoot;
    private final String baseUrl;

    public FileStorageService(
            @Value("${app.uploads.dir:./uploads}") String uploadDir,
            @Value("${app.uploads.base-url:http://localhost:8080/uploads}") String baseUrl) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        try {
            Files.createDirectories(this.uploadRoot);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create upload directory: " + uploadDir, e);
        }
    }

    /**
     * Validate content-type and size, then store the file under {@code subfolder}.
     *
     * @return the public URL of the stored file
     */
    public String validateAndStore(MultipartFile file, Set<String> allowedTypes, long maxMb, String subfolder) {
        validate(file, allowedTypes, maxMb);
        return baseUrl + "/" + subfolder + "/" + storeInternal(file, subfolder);
    }

    public String validateAndStoreProtected(MultipartFile file, Set<String> allowedTypes, long maxMb, String subfolder) {
        validate(file, allowedTypes, maxMb);
        return "/api/v1/files/" + subfolder + "/" + storeInternal(file, subfolder);
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
        return baseUrl + "/" + subfolder + "/" + storeInternal(file, subfolder);
    }

    private String storeInternal(MultipartFile file, String subfolder) {
        Path dir = safeResolve(subfolder);
        String ext = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains("."))
            ext = original.substring(original.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        String filename = UUID.randomUUID() + ext;
        try {
            Files.createDirectories(dir);
            Path destination = dir.resolve(filename).normalize();
            if (!destination.startsWith(uploadRoot)) throw new IOException("Invalid destination");
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file");
        }
        return filename;
    }

    public Path load(String relativePath) {
        Path file = safeResolve(relativePath);
        if (!Files.isRegularFile(file)) throw new ApiException(HttpStatus.NOT_FOUND, "File not found");
        return file;
    }

    private Path safeResolve(String relativePath) {
        if (relativePath == null || relativePath.isBlank() || relativePath.contains("..") || relativePath.startsWith("/")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid file path");
        }
        Path resolved = uploadRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(uploadRoot)) throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid file path");
        return resolved;
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
