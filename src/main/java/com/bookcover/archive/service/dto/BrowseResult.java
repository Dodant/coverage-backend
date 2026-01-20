package com.bookcover.archive.service.dto;

import java.util.List;

/**
 * 브라우징 결과 DTO.
 * 국가별, 연도별, 장르별, 태그별 브라우징 결과를 담습니다.
 *
 * Requirements:
 * - 8.5: 국가별 브라우징 페이지를 제공하여 특정 국가의 모든 북커버를 조회
 * - 8.6: 연도별 브라우징 페이지를 제공하여 특정 연도의 모든 북커버를 조회
 * - 8.7: 장르별 브라우징 페이지를 제공하여 특정 장르의 모든 북커버를 조회
 * - 8.8: 국가별/연도별/장르별 페이지를 조회하면 해당 카테고리의 북커버 수와 목록을 표시
 * - 20.6: 태그별 브라우징 페이지를 제공
 */
public record BrowseResult(
        String category,
        String categoryValue,
        long totalCount,
        List<BrowseResultItem> items,
        int page,
        int size,
        int totalPages
) {
    /**
     * 빈 브라우징 결과를 생성합니다.
     */
    public static BrowseResult empty(String category, String categoryValue) {
        return new BrowseResult(category, categoryValue, 0, List.of(), 0, 0, 0);
    }

    /**
     * 브라우징 결과가 비어있는지 확인합니다.
     */
    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }
}
