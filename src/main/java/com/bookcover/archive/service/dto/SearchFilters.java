package com.bookcover.archive.service.dto;

import java.util.List;
import java.util.UUID;

/**
 * 검색 필터 DTO.
 * 검색 시 적용할 필터 조건을 담습니다.
 *
 * Requirements:
 * - 8.2: 나라, 언어, 연도, 출판사, 장르별로 결과를 필터링
 */
public record SearchFilters(
        String countryCode,
        String languageCode,
        Integer yearFrom,
        Integer yearTo,
        String genre,
        UUID publisherId,
        List<UUID> tagIds,
        SortOption sortBy
) {
    public SearchFilters {
        if (sortBy == null) {
            sortBy = SortOption.RELEVANCE;
        }
    }

    /**
     * 빈 필터를 생성합니다.
     */
    public static SearchFilters empty() {
        return new SearchFilters(null, null, null, null, null, null, null, SortOption.RELEVANCE);
    }
}
