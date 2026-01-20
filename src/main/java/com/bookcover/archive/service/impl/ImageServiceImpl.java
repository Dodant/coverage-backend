package com.bookcover.archive.service.impl;

import com.bookcover.archive.service.ImageService;
import com.bookcover.archive.validation.ImageValidator;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * 이미지 처리 서비스 구현체.
 * S3/MinIO 업로드, 썸네일 생성, 색상 추출, sRGB 변환 기능을 제공합니다.
 *
 * Requirements:
 * - 14.7: 업로드된 이미지를 표준 색공간(sRGB)으로 변환하여 저장
 * - 14.8: 썸네일과 원본 이미지를 별도로 저장하여 로딩 성능 최적화
 * - 20.2: 대표 색상 추출 로직
 */
@Service
public class ImageServiceImpl implements ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageServiceImpl.class);

    private final S3Client s3Client;
    private final ImageValidator imageValidator;

    @Value("${s3.bucket-name:bookcovers}")
    private String bucketName;

    @Value("${s3.endpoint:http://localhost:9000}")
    private String s3Endpoint;

    @Value("${image.thumbnail.width:300}")
    private int thumbnailWidth;

    @Value("${image.thumbnail.height:450}")
    private int thumbnailHeight;

    public ImageServiceImpl(S3Client s3Client, ImageValidator imageValidator) {
        this.s3Client = s3Client;
        this.imageValidator = imageValidator;
    }

    @Override
    public ImageUploadResult uploadImage(MultipartFile file, ImageType type) {
        // Validate image first
        ImageValidationResult validationResult = validateImage(file);
        if (!validationResult.valid()) {
            throw new IllegalArgumentException("Image validation failed: " + String.join(", ", validationResult.errors()));
        }

        try {
            // Read and convert to sRGB
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            BufferedImage srgbImage = convertToSRGB(originalImage);

            // Generate unique file names
            String fileId = UUID.randomUUID().toString();
            String extension = getFileExtension(file.getOriginalFilename());
            String folder = type.name().toLowerCase();
            String originalKey = folder + "/" + fileId + "." + extension;
            String thumbnailKey = folder + "/thumbnails/" + fileId + "_thumb." + extension;

            // Upload original image (converted to sRGB)
            byte[] originalBytes = imageToBytes(srgbImage, extension);
            uploadToS3(originalKey, originalBytes, file.getContentType());

            // Generate and upload thumbnail
            BufferedImage thumbnail = generateThumbnail(srgbImage);
            byte[] thumbnailBytes = imageToBytes(thumbnail, extension);
            uploadToS3(thumbnailKey, thumbnailBytes, file.getContentType());

            String originalUrl = buildUrl(originalKey);
            String thumbnailUrl = buildUrl(thumbnailKey);

            log.info("Image uploaded successfully: {}", originalUrl);

            return new ImageUploadResult(
                    originalUrl,
                    thumbnailUrl,
                    srgbImage.getWidth(),
                    srgbImage.getHeight(),
                    originalBytes.length
            );
        } catch (IOException e) {
            log.error("Failed to upload image", e);
            throw new RuntimeException("Failed to upload image: " + e.getMessage(), e);
        }
    }

    @Override
    public ImageValidationResult validateImage(MultipartFile file) {
        ImageValidator.ImageValidationResult result = imageValidator.validate(file);
        return new ImageValidationResult(result.valid(), result.errors());
    }

    @Override
    public String extractDominantColor(String imageUrl) {
        try {
            String key = extractKeyFromUrl(imageUrl);
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            try (InputStream inputStream = s3Client.getObject(getRequest)) {
                BufferedImage image = ImageIO.read(inputStream);
                return extractDominantColorFromImage(image);
            }
        } catch (Exception e) {
            log.error("Failed to extract dominant color from image: {}", imageUrl, e);
            return null;
        }
    }

    @Override
    public void deleteImage(String imageUrl) {
        try {
            String key = extractKeyFromUrl(imageUrl);
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("Image deleted successfully: {}", imageUrl);
        } catch (Exception e) {
            log.error("Failed to delete image: {}", imageUrl, e);
            throw new RuntimeException("Failed to delete image: " + e.getMessage(), e);
        }
    }

    /**
     * 이미지를 sRGB 색공간으로 변환합니다.
     * ColorSpaceConverter 유틸리티를 사용합니다.
     * Requirements: 14.7
     */
    BufferedImage convertToSRGB(BufferedImage image) {
        return ColorSpaceConverter.convertToSRGB(image);
    }

    /**
     * 썸네일을 생성합니다.
     * Requirements: 14.8
     */
    BufferedImage generateThumbnail(BufferedImage image) {
        return Scalr.resize(image, Scalr.Method.QUALITY, Scalr.Mode.FIT_TO_WIDTH,
                thumbnailWidth, thumbnailHeight, Scalr.OP_ANTIALIAS);
    }

    /**
     * 이미지에서 대표 색상을 추출합니다.
     * ColorExtractor 유틸리티를 사용합니다.
     * Requirements: 20.2
     */
    String extractDominantColorFromImage(BufferedImage image) {
        return ColorExtractor.extractDominantColor(image);
    }

    /**
     * S3에 파일을 업로드합니다.
     */
    private void uploadToS3(String key, byte[] data, String contentType) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();
        s3Client.putObject(putRequest, RequestBody.fromBytes(data));
    }

    /**
     * BufferedImage를 바이트 배열로 변환합니다.
     */
    private byte[] imageToBytes(BufferedImage image, String extension) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String formatName = extension.equalsIgnoreCase("jpg") ? "jpeg" : extension;
        ImageIO.write(image, formatName, baos);
        return baos.toByteArray();
    }

    /**
     * S3 URL을 생성합니다.
     */
    private String buildUrl(String key) {
        return s3Endpoint + "/" + bucketName + "/" + key;
    }

    /**
     * URL에서 S3 키를 추출합니다.
     */
    private String extractKeyFromUrl(String url) {
        String prefix = s3Endpoint + "/" + bucketName + "/";
        if (url.startsWith(prefix)) {
            return url.substring(prefix.length());
        }
        // Handle case where URL might be different format
        int lastSlashIndex = url.lastIndexOf('/');
        if (lastSlashIndex > 0) {
            String path = url.substring(url.indexOf(bucketName) + bucketName.length() + 1);
            return path;
        }
        return url;
    }

    /**
     * 파일 확장자를 추출합니다.
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
