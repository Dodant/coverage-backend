package com.bookcover.archive.service.dto;

import com.bookcover.archive.domain.book.ImageSource;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * 번역서 수정 요청 DTO.
 * 모든 필드는 선택적이며, 제공된 필드만 업데이트됩니다.
 */
public record UpdateTranslatedBookRequest(
    String title,

    @Size(min = 2, max = 2, message = "국가 코드는 2자리여야 합니다")
    String countryCode,

    @Size(min = 2, max = 2, message = "언어 코드는 2자리여야 합니다")
    String languageCode,

    Integer year,
    String isbn,

    UUID authorId,
    UUID translatorId,
    UUID publisherId,
    UUID designerId,

    String edition,

    BookCoverUpdateInfo frontCover,
    BookCoverUpdateInfo backCover
) {
    /**
     * 북커버 수정 정보 DTO.
     */
    public record BookCoverUpdateInfo(
        String imageUrl,
        String thumbnailUrl,
        String originalFileName,
        Integer width,
        Integer height,
        Long fileSize,
        ImageSource imageSource,
        String sourceUrl
    ) {}
}
