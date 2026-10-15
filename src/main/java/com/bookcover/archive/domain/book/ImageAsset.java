package com.bookcover.archive.domain.book;

import com.bookcover.archive.service.ImageService.ImageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * 검증을 마친 업로드와 삭제 요청으로 차단된 이미지의 기록.
 *
 * Requirements: 14.1, 14.4, 17.3
 */
@Entity
@Table(name = "image_assets")
public class ImageAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 1000)
    private String imageUrl;

    @Column(length = 1000)
    private String thumbnailUrl;

    @Column(length = 64)
    private String sha256;

    @Enumerated(EnumType.STRING)
    private ImageType type;

    private Integer width;
    private Integer height;
    private Long fileSize;

    @Column(nullable = false)
    private boolean blocked;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
    public ImageType getType() { return type; }
    public void setType(ImageType type) { this.type = type; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
}
