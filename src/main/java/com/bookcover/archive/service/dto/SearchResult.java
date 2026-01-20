package com.bookcover.archive.service.dto;

import java.util.List;

/**
 * 검색 결과 DTO.
 *
 * Requirements:
 * - 8.1: 책 제목, 작가, 출판사, 디자이너 이름, ISBN으로 검색 결과를 반환
 * - 8.4: 검색 결과가 없으면 적절한 안내 메시지를 표시
 */
public record SearchResult(
        List<SearchResultItem> items,
        long totalCount,
        SearchFacets facets,
        int page,
        int size,
        int totalPages
) {
    /**
     * 빈 검색 결과를 생성합니다.
     */
    public static SearchResult empty() {
        return new SearchResult(List.of(), 0, SearchFacets.empty(), 0, 0, 0);
    }

    /**
     * 검색 결과가 비어있는지 확인합니다.
     */
    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }
}
