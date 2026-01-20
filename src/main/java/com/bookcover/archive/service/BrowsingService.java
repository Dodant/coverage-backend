package com.bookcover.archive.service;

import com.bookcover.archive.service.dto.BrowseResult;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 브라우징 서비스 인터페이스.
 * 국가별, 연도별, 장르별, 태그별 브라우징 기능을 제공합니다.
 *
 * Requirements:
 * - 8.5: 국가별 브라우징 페이지를 제공하여 특정 국가의 모든 북커버를 조회
 * - 8.6: 연도별 브라우징 페이지를 제공하여 특정 연도의 모든 북커버를 조회
 * - 8.7: 장르별 브라우징 페이지를 제공하여 특정 장르의 모든 북커버를 조회
 * - 8.8: 국가별/연도별/장르별 페이지를 조회하면 해당 카테고리의 북커버 수와 목록을 표시
 * - 20.6: 태그별 브라우징 페이지를 제공
 */
public interface BrowsingService {

    /**
     * 국가별 브라우징을 수행합니다.
     *
     * @param countryCode 국가 코드 (ISO 3166-1)
     * @param pageable 페이징 정보
     * @return 브라우징 결과
     */
    BrowseResult browseByCountry(String countryCode, Pageable pageable);

    /**
     * 연도별 브라우징을 수행합니다.
     *
     * @param year 출판 연도
     * @param pageable 페이징 정보
     * @return 브라우징 결과
     */
    BrowseResult browseByYear(int year, Pageable pageable);

    /**
     * 장르별 브라우징을 수행합니다.
     *
     * @param genre 장르
     * @param pageable 페이징 정보
     * @return 브라우징 결과
     */
    BrowseResult browseByGenre(String genre, Pageable pageable);

    /**
     * 태그별 브라우징을 수행합니다.
     *
     * @param tagId 태그 ID
     * @param pageable 페이징 정보
     * @return 브라우징 결과
     */
    BrowseResult browseByTag(UUID tagId, Pageable pageable);

    /**
     * 모든 국가 목록과 각 국가의 북커버 수를 조회합니다.
     *
     * @return 국가 코드와 북커버 수 맵
     */
    Map<String, Long> getCountryCounts();

    /**
     * 모든 연도 목록과 각 연도의 북커버 수를 조회합니다.
     *
     * @return 연도와 북커버 수 맵
     */
    Map<Integer, Long> getYearCounts();

    /**
     * 모든 장르 목록과 각 장르의 북커버 수를 조회합니다.
     *
     * @return 장르와 북커버 수 맵
     */
    Map<String, Long> getGenreCounts();

    /**
     * 인기 태그 목록을 조회합니다.
     *
     * @param limit 조회할 태그 수
     * @return 태그 정보 목록
     */
    List<TagInfo> getPopularTags(int limit);

    /**
     * 태그 정보 DTO
     */
    record TagInfo(UUID id, String name, String category, long count) {}
}
