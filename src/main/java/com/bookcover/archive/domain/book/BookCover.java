package com.bookcover.archive.domain.book;

import com.bookcover.archive.domain.media.Media;
import com.bookcover.archive.domain.tag.DesignTag;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.ManyToMany;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 북커버 엔티티.
 * 책의 앞면 또는 뒷면 커버 이미지 정보를 저장합니다.
 *
 * Requirements:
 * - 1.4: 뒷면 북커버가 제공되는 경우 해당 이미지를 선택적으로 저장
 * - 14.8: 썸네일과 원본 이미지를 별도로 저장하여 로딩 성능 최적화
 */
@Entity
@Table(name = "book_covers")
public class BookCover {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** 원본 이미지 URL */
    @Column(nullable = false)
    private String imageUrl;

    /** 썸네일 이미지 URL */
    @Column(nullable = false)
    private String thumbnailUrl;

    /** 원본 파일명 */
    @Column(nullable = false)
    private String originalFileName;

    /** 이미지 너비 (픽셀) */
    private Integer width;

    /** 이미지 높이 (픽셀) */
    private Integer height;

    /** 파일 크기 (바이트) */
    private Long fileSize;

    /** 커버 유형 (앞면/뒷면) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CoverType coverType;

    /** 이미지 출처 */
    @Enumerated(EnumType.STRING)
    private ImageSource imageSource;

    /** 출처 URL (링크에서 가져온 경우). 게시중단 요청 시 원 출처 추적용 */
    @Column(length = 1000)
    private String sourceUrl;

    /** 이미지 제공처 이름 (예: Open Library, Penguin Random House) */
    @Column(length = 100)
    private String sourceName;

    /** 언어가 달라도 디자인이 비슷한 표지끼리 같은 값. 관리자가 백오피스에서 묶는다(없으면 null) */
    @Column(name = "similar_group_id")
    private UUID similarGroupId;

    /** 대표 색상 (HEX 코드) */
    @Column(length = 7)
    private String dominantColor;

    /** 레이아웃 유형 */
    @Enumerated(EnumType.STRING)
    private CoverLayout layout;

    /** 좋아요 수 */
    @Column(nullable = false)
    private Integer likeCount = 0;

    /** 댓글 수 */
    @Column(nullable = false)
    private Integer commentCount = 0;

    /** 저작권 삭제 요청으로 공개가 중지된 표지. */
    @Column(nullable = false)
    private boolean hidden = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** 이 표지가 속한 책 (같은 ISBN의 표지 변형들을 묶음). 뒷면 커버는 null */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Media book;

    /** 표지 변형 설명 (예: "10쇄 리커버", "영화 개봉 특별판") */
    @Column(length = 100)
    private String label;

    /** 이 표지가 처음 쓰인 쇄 */
    private Integer printingFrom;

    /** 이 표지가 쓰이기 시작한 날짜 */
    private LocalDate usedFrom;

    /** 표지가 쓰인 나라 (ISO 3166-1). null이면 책의 나라를 따른다 */
    @Column(length = 2)
    private String countryCode;

    /** 표지 언어 (ISO 639-1). null이면 책의 언어를 따른다 */
    @Column(length = 2)
    private String languageCode;

    /** 역할별 크레딧 (디자인·일러스트·사진 등) */
    @OneToMany(mappedBy = "cover", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CoverCredit> credits = new ArrayList<>();

    /** Requirements: 20.1 - 표지에 부여된 디자인 태그. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "book_cover_tags", joinColumns = @JoinColumn(name = "cover_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<DesignTag> tags = new LinkedHashSet<>();

    public Set<DesignTag> getTags() { return tags; }

    public List<CoverCredit> getCredits() {
        return credits;
    }

    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public Media getBook() {
        return book;
    }

    public void setBook(Media book) {
        this.book = book;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getPrintingFrom() {
        return printingFrom;
    }

    public void setPrintingFrom(Integer printingFrom) {
        this.printingFrom = printingFrom;
    }

    public LocalDate getUsedFrom() {
        return usedFrom;
    }

    public void setUsedFrom(LocalDate usedFrom) {
        this.usedFrom = usedFrom;
    }

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

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public CoverType getCoverType() {
        return coverType;
    }

    public void setCoverType(CoverType coverType) {
        this.coverType = coverType;
    }

    public ImageSource getImageSource() {
        return imageSource;
    }

    public void setImageSource(ImageSource imageSource) {
        this.imageSource = imageSource;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public UUID getSimilarGroupId() {
        return similarGroupId;
    }

    public void setSimilarGroupId(UUID similarGroupId) {
        this.similarGroupId = similarGroupId;
    }

    public String getDominantColor() {
        return dominantColor;
    }

    public void setDominantColor(String dominantColor) {
        this.dominantColor = dominantColor;
    }

    public CoverLayout getLayout() {
        return layout;
    }

    public void setLayout(CoverLayout layout) {
        this.layout = layout;
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
}
