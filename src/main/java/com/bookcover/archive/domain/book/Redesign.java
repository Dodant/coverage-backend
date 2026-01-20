package com.bookcover.archive.domain.book;

import com.bookcover.archive.domain.common.ContentStatus;
import com.bookcover.archive.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * 리디자인 엔티티.
 * 회원이 기존 북커버를 재해석하여 업로드한 디자인 작품입니다.
 *
 * Requirements:
 * - 3.1: 리디자인을 업로드하면 해당 리디자인을 원서 또는 번역서와 연결
 * - 3.7: 리디자인이 등록되면 업로드한 회원 정보를 리디자인과 연결하여 저장
 */
@Entity
@Table(name = "redesigns")
public class Redesign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** 리디자인 이미지 URL */
    @Column(nullable = false)
    private String imageUrl;

    /** 썸네일 이미지 URL */
    @Column(nullable = false)
    private String thumbnailUrl;

    /** 리디자인 설명 */
    @Column(nullable = false, length = 2000)
    private String description;

    /** 연결된 원서 (원서 또는 번역서 중 하나와 연결) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_book_id")
    private OriginalBook originalBook;

    /** 연결된 번역서 (원서 또는 번역서 중 하나와 연결) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "translated_book_id")
    private TranslatedBook translatedBook;

    /** 업로드한 회원 (필수) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    /** 콘텐츠 상태 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentStatus status = ContentStatus.PUBLISHED;

    /** 좋아요 수 */
    @Column(nullable = false)
    private Integer likeCount = 0;

    /** 댓글 수 */
    @Column(nullable = false)
    private Integer commentCount = 0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OriginalBook getOriginalBook() {
        return originalBook;
    }

    public void setOriginalBook(OriginalBook originalBook) {
        this.originalBook = originalBook;
    }

    public TranslatedBook getTranslatedBook() {
        return translatedBook;
    }

    public void setTranslatedBook(TranslatedBook translatedBook) {
        this.translatedBook = translatedBook;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public ContentStatus getStatus() {
        return status;
    }

    public void setStatus(ContentStatus status) {
        this.status = status;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 좋아요 수를 증가시킵니다.
     */
    public void incrementLikeCount() {
        this.likeCount++;
    }

    /**
     * 좋아요 수를 감소시킵니다.
     */
    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    /**
     * 댓글 수를 증가시킵니다.
     */
    public void incrementCommentCount() {
        this.commentCount++;
    }

    /**
     * 댓글 수를 감소시킵니다.
     */
    public void decrementCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }

    /**
     * 리디자인이 원서 또는 번역서와 연결되어 있는지 확인합니다.
     * @return 연결되어 있으면 true
     */
    public boolean hasBookReference() {
        return originalBook != null || translatedBook != null;
    }
}
