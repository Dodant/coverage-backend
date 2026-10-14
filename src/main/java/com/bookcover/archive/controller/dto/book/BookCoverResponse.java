package com.bookcover.archive.controller.dto.book;

import com.bookcover.archive.domain.book.BookCover;
import com.bookcover.archive.domain.book.CoverLayout;
import com.bookcover.archive.domain.book.CoverType;
import com.bookcover.archive.domain.book.ImageSource;

import java.time.Instant;
import java.time.LocalDate;
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
    String dominantColor,
    CoverLayout layout,
    Integer likeCount,
    Integer commentCount,
    Instant createdAt,
    String label,
    Integer printingFrom,
    LocalDate usedFrom
) {
    public static BookCoverResponse from(BookCover cover) {
        if (cover == null) {
            return null;
        }
        return new BookCoverResponse(
            cover.getId(),
            cover.getImageUrl(),
            cover.getThumbnailUrl(),
            cover.getOriginalFileName(),
            cover.getWidth(),
            cover.getHeight(),
            cover.getFileSize(),
            cover.getCoverType(),
            cover.getImageSource(),
            cover.getSourceUrl(),
            cover.getDominantColor(),
            cover.getLayout(),
            cover.getLikeCount(),
            cover.getCommentCount(),
            cover.getCreatedAt(),
            cover.getLabel(),
            cover.getPrintingFrom(),
            cover.getUsedFrom()
        );
    }
}
