package com.bookcover.archive.service.dto;

import com.bookcover.archive.domain.book.ImageSource;
import com.bookcover.archive.domain.common.ContentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * 원서 등록 요청 DTO.
 *
 * Requirements:
 * - 1.1: 책 제목, 언어, 앞면 북커버는 필수. 나라·연도·장르는 모르면 생략할 수 있다
 *        (원서·번역서 비교가 목적이라 서지가 덜 갖춰진 판도 받는다. 2026-10 변경). 원서 제목이 없으면 책 제목을 쓴다
 * - 1.2: ISBN이 제공되는 경우 ISBN 형식을 검증하고 저장
 * - 1.6: 북커버 디자이너 정보는 표지별 크레딧(BookCoverInfo.credits)으로 저장
 * - 1.7: 에디션 정보가 제공되면 에디션 정보를 원서와 연결하여 저장
 */
public record CreateOriginalBookRequest(
    @NotBlank(message = "책 제목은 필수입니다")
    String title,

    String originalTitle,

    @Size(min = 2, max = 2, message = "국가 코드는 2자리여야 합니다")
    String countryCode,

    @NotBlank(message = "언어 코드는 필수입니다")
    @Size(min = 2, max = 2, message = "언어 코드는 2자리여야 합니다")
    String languageCode,

    Integer year,

    String genre,

    String isbn,

    UUID authorId,
    String authorName,

    UUID publisherId,
    String publisherName,

    String edition,

    UUID seriesId,
    /** seriesId가 없으면 출판사 안에서 이름으로 찾거나 새로 만든다 */
    String seriesName,
    @Size(max = 20, message = "시리즈 권 번호는 20자 이하여야 합니다")
    String seriesNumber,


    UUID workId,

    @NotNull(message = "앞면 북커버 정보는 필수입니다")
    @Valid
    BookCoverInfo frontCover,

    @Valid
    BookCoverInfo backCover,

    UUID createdById,

    /** DRAFT면 관리자가 검토해 공개할 때까지 사용자에게 안 보인다(백오피스 '퍼블리시'). 그 외·null은 PUBLISHED */
    ContentStatus status
) {
    public CreateOriginalBookRequest {
        if (originalTitle == null || originalTitle.isBlank()) {
            originalTitle = title;
        }
    }

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
