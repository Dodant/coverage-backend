package com.bookcover.archive.validation;

import com.bookcover.archive.config.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Validator for image files.
 * Validates resolution, file format, and file size.
 *
 * image.resolution-exempt-user-ids에 있는 사용자(서지 자동 등록 에이전트 계정)는 최소 해상도 검사를 건너뛴다.
 * 공개 서지 API 표지는 대부분 500px 안팎이라 규칙을 지키면 등록할 수 없기 때문이다.
 *
 * Validates: Requirements 14.1, 14.4, 14.5
 */
@Component
public class ImageValidator {

    public static final int MIN_WIDTH = 800;
    public static final int MIN_HEIGHT = 1200;
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    public static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    public static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp"
    );

    @Value("${image.resolution-exempt-user-ids:}")
    private Set<UUID> resolutionExemptUserIds = Set.of();

    /**
     * Validates an image file for all criteria.
     *
     * @param file the multipart file to validate
     * @return validation result containing validity status and any errors
     */
    public ImageValidationResult validate(MultipartFile file) {
        List<String> errors = new ArrayList<>();

        if (file == null || file.isEmpty()) {
            errors.add("Image file is required");
            return new ImageValidationResult(false, errors, null, null);
        }

        // Validate file format
        if (!isValidFormat(file)) {
            errors.add("Invalid file format. Allowed formats: JPEG, PNG, WebP");
        }

        // Validate file size
        if (!isValidFileSize(file)) {
            errors.add("File size exceeds maximum limit of 10MB");
        }

        // Validate resolution
        ImageDimensions dimensions = null;
        try {
            dimensions = getImageDimensions(file);
            if (dimensions != null && !isValidResolution(dimensions.width(), dimensions.height())
                    && !isResolutionExempt()) {
                errors.add(String.format(
                        "Image resolution too low. Minimum required: %dx%d pixels. Provided: %dx%d pixels",
                        MIN_WIDTH, MIN_HEIGHT, dimensions.width(), dimensions.height()
                ));
            }
        } catch (IOException e) {
            errors.add("Unable to read image dimensions");
        }

        return new ImageValidationResult(errors.isEmpty(), errors,
                dimensions != null ? dimensions.width() : null,
                dimensions != null ? dimensions.height() : null);
    }

    /**
     * Validates if the file format is allowed (JPEG, PNG, WebP).
     *
     * @param file the multipart file to check
     * @return true if format is valid, false otherwise
     */
    public boolean isValidFormat(MultipartFile file) {
        if (file == null) {
            return false;
        }

        String contentType = file.getContentType();
        if (contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            return true;
        }

        String filename = file.getOriginalFilename();
        if (filename != null) {
            String extension = getFileExtension(filename).toLowerCase();
            return ALLOWED_EXTENSIONS.contains(extension);
        }

        return false;
    }

    /**
     * Validates if the file size is within the allowed limit (10MB).
     *
     * @param file the multipart file to check
     * @return true if size is valid, false otherwise
     */
    public boolean isValidFileSize(MultipartFile file) {
        if (file == null) {
            return false;
        }
        return file.getSize() <= MAX_FILE_SIZE;
    }

    /**
     * Validates if the image resolution meets minimum requirements (800x1200).
     *
     * @param width  the image width in pixels
     * @param height the image height in pixels
     * @return true if resolution is valid, false otherwise
     */
    public boolean isValidResolution(int width, int height) {
        return width >= MIN_WIDTH && height >= MIN_HEIGHT;
    }

    private boolean isResolutionExempt() {
        return SecurityUtils.getCurrentUserId().map(resolutionExemptUserIds::contains).orElse(false);
    }

    /**
     * Gets the dimensions of an image file.
     *
     * @param file the multipart file
     * @return ImageDimensions containing width and height
     * @throws IOException if the image cannot be read
     */
    public ImageDimensions getImageDimensions(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IOException("Unable to read image");
            }
            return new ImageDimensions(image.getWidth(), image.getHeight());
        }
    }

    /**
     * Extracts the file extension from a filename.
     *
     * @param filename the filename
     * @return the file extension without the dot
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    /**
     * Record representing image dimensions.
     */
    public record ImageDimensions(int width, int height) {}

    /**
     * Record representing the result of image validation.
     */
    public record ImageValidationResult(
            boolean valid,
            List<String> errors,
            Integer width,
            Integer height
    ) {}
}
