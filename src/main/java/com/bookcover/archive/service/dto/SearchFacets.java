package com.bookcover.archive.service.dto;

import java.util.Map;

/**
 * 검색 결과 패싯 DTO.
 * 검색 결과의 집계 정보를 담습니다.
 */
public record SearchFacets(
        Map<String, Long> countries,
        Map<Integer, Long> years,
        Map<String, Long> genres,
        Map<String, Long> tags
) {
    /**
     * 빈 패싯을 생성합니다.
     */
    public static SearchFacets empty() {
        return new SearchFacets(Map.of(), Map.of(), Map.of(), Map.of());
    }
}
