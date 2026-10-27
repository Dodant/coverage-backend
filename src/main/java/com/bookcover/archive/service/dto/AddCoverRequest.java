package com.bookcover.archive.service.dto;

import com.bookcover.archive.domain.book.ImageSource;
import com.bookcover.archive.validation.annotation.ValidCountryCode;
import com.bookcover.archive.validation.annotation.ValidLanguageCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

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
    @Size(max = 1000, message = "출처 URL은 1000자 이하여야 합니다")
    String sourceUrl,

    /** 이미지 제공처 이름 (예: Open Library, Penguin Random House) */
    @Size(max = 100, message = "출처 이름은 100자 이하여야 합니다")
    String sourceName,

    @Size(max = 100, message = "표지 설명은 100자 이하여야 합니다")
    String label,

    @Positive(message = "쇄는 1 이상이어야 합니다")
    Integer printingFrom,

    LocalDate usedFrom,

    /** 표지가 쓰인 나라 (ISO 3166-1). 없으면 책의 나라를 따른다 */
    @ValidCountryCode(optional = true, message = "올바른 국가 코드가 아닙니다")
    String countryCode,

    /** 표지 언어 (ISO 639-1). 없으면 책의 언어를 따른다 */
    @ValidLanguageCode(optional = true, message = "올바른 언어 코드가 아닙니다")
    String languageCode,

    /** 역할별 크레딧 */
    List<@Valid CreditInfo> credits,

    com.bookcover.archive.domain.book.CoverVariantType variantType,
    com.bookcover.archive.domain.book.RibbonStatus ribbonStatus
) {}
