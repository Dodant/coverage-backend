package com.bookcover.archive.service.dto;

/**
 * 검색 결과 정렬 옵션.
 *
 * Requirements:
 * - 8.3: 검색 결과를 좋아요 수, 등록일, 연도순으로 정렬
 */
public enum SortOption {
    /** 관련성 순 (기본값) */
    RELEVANCE,
    /** 좋아요 수 내림차순 */
    LIKES,
    /** 최신 등록순 */
    NEWEST,
    /** 연도 오름차순 */
    YEAR_ASC,
    /** 연도 내림차순 */
    YEAR_DESC
}
