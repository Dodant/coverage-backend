package com.bookcover.archive.service.impl;

import com.bookcover.archive.domain.book.ImageAsset;
import com.bookcover.archive.exception.ValidationException;
import com.bookcover.archive.repository.ImageAssetRepository;
import com.bookcover.archive.service.ImageService;
import com.bookcover.archive.validation.ImageValidator;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
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
    private final ImageAssetRepository imageAssetRepository;

    @Value("${s3.bucket-name:bookcovers}")
    private String bucketName;

    @Value("${s3.endpoint:http://localhost:9000}")
    private String s3Endpoint;

    @Value("${image.thumbnail.width:300}")
    private int thumbnailWidth;

    @Value("${image.thumbnail.height:450}")
    private int thumbnailHeight;

    public ImageServiceImpl(S3Client s3Client, ImageValidator imageValidator,
                            ImageAssetRepository imageAssetRepository) {
        this.s3Client = s3Client;
        this.imageValidator = imageValidator;
        this.imageAssetRepository = imageAssetRepository;
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
            String hash = pixelHash(srgbImage);
            if (imageAssetRepository.existsBySha256AndBlockedTrue(hash)) {
                throw new ValidationException("삭제 요청으로 차단된 이미지입니다", Map.of("file", "재업로드할 수 없습니다"));
            }

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

            ImageAsset asset = new ImageAsset();
            asset.setImageUrl(originalUrl);
            asset.setThumbnailUrl(thumbnailUrl);
            asset.setSha256(hash);
            asset.setType(type);
            asset.setWidth(srgbImage.getWidth());
            asset.setHeight(srgbImage.getHeight());
            asset.setFileSize((long) originalBytes.length);
            imageAssetRepository.save(asset);

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
    @Transactional(readOnly = true)
    public void requireVerifiedImage(String imageUrl, String thumbnailUrl, ImageType type,
                                     Integer width, Integer height, Long fileSize) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new ValidationException("이미지 URL이 필요합니다", Map.of("imageUrl", "필수입니다"));
        }
        ImageAsset asset = imageAssetRepository.findByImageUrl(imageUrl)
                .orElseThrow(() -> new ValidationException("검증된 업로드 이미지가 아닙니다",
                        Map.of("imageUrl", imageUrl)));
        if (asset.isBlocked() || (asset.getSha256() != null
                && imageAssetRepository.existsBySha256AndBlockedTrue(asset.getSha256())) || asset.getType() != type
                || !Objects.equals(asset.getThumbnailUrl(), thumbnailUrl)
                || (width != null && !Objects.equals(asset.getWidth(), width))
                || (height != null && !Objects.equals(asset.getHeight(), height))
                || (fileSize != null && !Objects.equals(asset.getFileSize(), fileSize))) {
            throw new ValidationException("이미지 정보가 업로드 기록과 일치하지 않습니다",
                    Map.of("imageUrl", imageUrl));
        }
    }

    @Override
    @Transactional
    public void blockImage(String imageUrl, String thumbnailUrl) {
        // ponytail: S3 삭제와 DB 커밋은 분산 트랜잭션이 아니므로 부분 실패 시 재시도가 필요하다.
        ImageAsset asset = imageAssetRepository.findByImageUrl(imageUrl).orElseGet(() -> {
            ImageAsset legacy = new ImageAsset();
            legacy.setImageUrl(imageUrl);
            legacy.setThumbnailUrl(thumbnailUrl);
            return legacy;
        });
        if (asset.getSha256() == null && isManagedUrl(imageUrl)) {
            try (InputStream input = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucketName).key(extractKeyFromUrl(imageUrl)).build())) {
                BufferedImage image = ImageIO.read(input);
                if (image != null) asset.setSha256(pixelHash(convertToSRGB(image)));
            } catch (Exception ex) {
                log.warn("기존 이미지의 해시를 읽지 못했습니다: {}", imageUrl, ex);
            }
        }
        // 같은 픽셀의 업로드가 여러 URL로 존재해도 모두 공개 중지한다.
        java.util.List<ImageAsset> copies = asset.getSha256() == null
                ? java.util.List.of(asset) : new java.util.ArrayList<>(imageAssetRepository.findAllBySha256(asset.getSha256()));
        if (copies.stream().noneMatch(copy -> Objects.equals(copy.getImageUrl(), imageUrl))) copies.add(asset);
        for (ImageAsset copy : copies) {
            copy.setBlocked(true);
            imageAssetRepository.save(copy);
            if (isManagedUrl(copy.getImageUrl())) deleteImage(copy.getImageUrl());
            if (isManagedUrl(copy.getThumbnailUrl())) deleteImage(copy.getThumbnailUrl());
        }
    }

    private boolean isManagedUrl(String url) {
        return url != null && url.startsWith(s3Endpoint + "/" + bucketName + "/");
    }

    private static String pixelHash(BufferedImage image) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            int width = image.getWidth();
            int height = image.getHeight();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = image.getRGB(x, y);
                    digest.update((byte) (pixel >>> 24));
                    digest.update((byte) (pixel >>> 16));
                    digest.update((byte) (pixel >>> 8));
                    digest.update((byte) pixel);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다", ex);
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
