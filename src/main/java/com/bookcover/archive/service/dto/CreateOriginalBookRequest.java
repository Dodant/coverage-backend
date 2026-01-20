package com.bookcover.archive.service.dto;

import com.bookcover.archive.domain.book.ImageSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * 원서 등록 요청 DTO.
 *
 * Requirements:
 * - 1.1: 책 제목, 원서 제목, 나라, 언어, 연도, 작가, 출판사, 장르, 앞면 북커버를 필수 항목으로 검증
 * - 1.2: ISBN이 제공되는 경우 ISBN 형식을 검증하고 저장
 * - 1.6: 북커버 디자이너 정보가 제공되면 디자이너 정보를 원서와 연결하여 저장
 * - 1.7: 에디션 정보가 제공되면 에디션 정보를 원서와 연결하여 저장
 */
public record CreateOriginalBookRequest(
    @NotBlank(message = "책 제목은 필수입니다")
    String title,

    @NotBlank(message = "원서 제목은 필수입니다")
    String originalTitle,

    @NotBlank(message = "국가 코드는 필수입니다")
    @Size(min = 2, max = 2, message = "국가 코드는 2자리여야 합니다")
    String countryCode,

    @NotBlank(message = "언어 코드는 필수입니다")
    @Size(min = 2, max = 2, message = "언어 코드는 2자리여야 합니다")
    String languageCode,

    @NotNull(message = "출판 연도는 필수입니다")
    Integer year,

    @NotBlank(message = "장르는 필수입니다")
    String genre,

    String isbn,

    UUID authorId,
    String authorName,

    UUID publisherId,
    String publisherName,

    UUID designerId,
    String designerName,

    String edition,

    UUID workId,

    @NotNull(message = "앞면 북커버 정보는 필수입니다")
    BookCoverInfo frontCover,

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
        String sourceUrl
    ) {}
}
