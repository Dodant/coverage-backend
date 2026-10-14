package com.bookcover.archive.service.dto;

import com.bookcover.archive.search.document.DocumentType;

import java.time.Instant;
import java.util.List;

/**
 * 검색 결과 항목 DTO.
 *
 * Requirements:
 * - 8.1: 책 제목, 작가, 출판사, 디자이너 이름, ISBN으로 검색 결과를 반환
 */
public record SearchResultItem(
        String id,
        DocumentType type,
        String title,
        String thumbnailUrl,
        String author,
        String publisher,
        List<String> designers,
        Integer year,
        String countryCode,
        String genre,
        Integer likeCount,
        Instant createdAt
) {
}
