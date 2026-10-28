package com.bookcover.archive.controller.dto.book;

import com.bookcover.archive.domain.book.BookCover;
import com.bookcover.archive.domain.book.CoverLayout;
import com.bookcover.archive.domain.book.CoverType;
import com.bookcover.archive.domain.book.CreditRole;
import com.bookcover.archive.domain.book.ImageSource;
import com.bookcover.archive.controller.dto.tag.TagResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 북커버 응답 DTO.
 */
public record BookCoverResponse(
    UUID id,
    String imageUrl,
    String thumbnailUrl,
    String originalFileName,
    Integer width,
    Integer height,
    Long fileSize,
    CoverType coverType,
    ImageSource imageSource,
    String sourceUrl,
    String sourceName,
    String dominantColor,
    CoverLayout layout,
    Integer likeCount,
    Integer commentCount,
    boolean hidden,
    Instant createdAt,
    String label,
    Integer printingFrom,
    LocalDate usedFrom,
    String countryCode,
    String languageCode,
    List<CreditResponse> credits,
    List<TagResponse> tags,
    /** 비슷한 표지 묶음 ID (같은 값이면 언어가 달라도 디자인이 비슷한 표지) */
    UUID similarGroupId,
    com.bookcover.archive.domain.book.CoverVariantType variantType,
    com.bookcover.archive.domain.book.RibbonStatus ribbonStatus
) {
    /** 표지 크레딧 (designerId는 /designers/{id} 프로필로 연결) */
    public record CreditResponse(UUID designerId, String name, CreditRole role) {}

    public static BookCoverResponse from(BookCover cover) {
        if (cover == null) {
            return null;
        }
        return new BookCoverResponse(
            cover.getId(),
            cover.isHidden() ? null : cover.getImageUrl(),
            cover.isHidden() ? null : cover.getThumbnailUrl(),
            cover.getOriginalFileName(),
            cover.getWidth(),
            cover.getHeight(),
            cover.getFileSize(),
            cover.getCoverType(),
            cover.getImageSource(),
            cover.getSourceUrl(),
            cover.getSourceName(),
            cover.getDominantColor(),
            cover.getLayout(),
            cover.getLikeCount(),
            cover.getCommentCount(),
            cover.isHidden(),
            cover.getCreatedAt(),
            cover.getLabel(),
            cover.getPrintingFrom(),
            cover.getUsedFrom(),
            cover.getCountryCode(),
            cover.getLanguageCode(),
            cover.getCredits().stream()
                .map(c -> new CreditResponse(c.getDesigner().getId(), c.getDesigner().getName(), c.getRole()))
                .toList(),
            cover.getTags().stream().filter(t -> Boolean.TRUE.equals(t.getApproved())).map(TagResponse::from).toList(),
            cover.getSimilarGroupId(),
            cover.getVariantType(),
            cover.getRibbonStatus()
        );
    }
}
