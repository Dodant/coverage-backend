package com.bookcover.archive.service.dto;

import com.bookcover.archive.domain.book.ImageSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 같은 책(ISBN)에 앞면 표지 변형을 추가하는 요청 DTO.
 * 예: 리커버, 쇄별로 바뀐 표지, 특별판 표지
 */
public record AddCoverRequest(
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
    String sourceUrl,

    @Size(max = 100, message = "표지 설명은 100자 이하여야 합니다")
    String label,

    @Positive(message = "쇄는 1 이상이어야 합니다")
    Integer printingFrom,

    LocalDate usedFrom
) {}
