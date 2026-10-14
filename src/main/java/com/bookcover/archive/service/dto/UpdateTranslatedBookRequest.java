package com.bookcover.archive.service.dto;

import com.bookcover.archive.domain.book.ImageSource;
import jakarta.validation.constraints.Size;

import java.util.List;
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

    String edition,
    UUID seriesId,

    @Size(max = 20, message = "시리즈 권 번호는 20자 이하여야 합니다")
    String seriesNumber,

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
        String sourceUrl,

        /** 주면 크레딧 전체를 교체한다 (빈 목록이면 모두 삭제). null이면 그대로 둔다 */
        List<CreditInfo> credits
    ) {}
}
