package com.bookcover.archive.service.dto;

import com.bookcover.archive.search.document.DocumentType;

import java.time.Instant;

/**
 * 브라우징 결과 항목 DTO.
 *
 * Requirements:
 * - 8.5, 8.6, 8.7, 20.6: 브라우징 결과 항목
 */
public record BrowseResultItem(
        String id,
        DocumentType type,
        String title,
        String thumbnailUrl,
        String author,
        String publisher,
        Integer year,
        String countryCode,
        String genre,
        Integer likeCount,
        Instant createdAt
) {
}
