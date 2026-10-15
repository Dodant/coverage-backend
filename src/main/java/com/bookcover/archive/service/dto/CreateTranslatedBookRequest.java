package com.bookcover.archive.service.dto;

import com.bookcover.archive.domain.book.ImageSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * 번역서 등록 요청 DTO.
 *
 * Requirements:
 * - 2.2: 각국 책 제목, 언어, 앞면 북커버는 필수. 나라·연도는 모르면 생략할 수 있다 (2026-10 변경)
 * - 2.3: ISBN이 제공되는 경우 ISBN 형식을 검증하고 저장
 * - 2.5: 에디션 정보가 제공되면 에디션 정보를 번역서와 연결하여 저장
 */
public record CreateTranslatedBookRequest(
    @NotBlank(message = "책 제목은 필수입니다")
    String title,

    @Size(min = 2, max = 2, message = "국가 코드는 2자리여야 합니다")
    String countryCode,

    @NotBlank(message = "언어 코드는 필수입니다")
    @Size(min = 2, max = 2, message = "언어 코드는 2자리여야 합니다")
    String languageCode,

    Integer year,

    String isbn,

    UUID authorId,
    String authorName,

    UUID translatorId,
    String translatorName,

    UUID publisherId,
    String publisherName,

    String edition,

    UUID seriesId,
    /** seriesId가 없으면 출판사 안에서 이름으로 찾거나 새로 만든다 */
    String seriesName,
    @Size(max = 20, message = "시리즈 권 번호는 20자 이하여야 합니다")
    String seriesNumber,


    @NotNull(message = "앞면 북커버 정보는 필수입니다")
    @Valid
    BookCoverInfo frontCover,

    @Valid
    BookCoverInfo backCover,

    UUID createdById
) {
    /**
     * 북커버 정보 DTO.
     */
    public record BookCoverInfo(
        @NotBlank(message = "이미지 URL은 필수입니다")
        String imageUrl,

        @NotBlank(message = "썸네일 URL은 필수입니다")
        String thumbnailUrl,

        @NotBlank(message = "원본 파일명은 필수입니다")
        String originalFileName,

        Integer width,
        Integer height,
        Long fileSize,
        ImageSource imageSource,
        @Size(max = 1000, message = "출처 URL은 1000자 이하여야 합니다")
        String sourceUrl,
        /** 이미지 제공처 이름 (예: Open Library, Penguin Random House) */
        @Size(max = 100, message = "출처 이름은 100자 이하여야 합니다")
        String sourceName,

        /** 역할별 크레딧 */
        List<CreditInfo> credits
    ) {}
}
