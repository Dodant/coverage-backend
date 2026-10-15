package com.bookcover.archive.service;

import com.bookcover.archive.search.document.BookCoverDocument;
import com.bookcover.archive.service.dto.SearchFilters;
import com.bookcover.archive.service.dto.SearchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * 검색 서비스 인터페이스.
 * 전문 검색, 필터링, 정렬 및 브라우징 기능을 제공합니다.
 *
 * Requirements:
 * - 8.1: 책 제목, 작가, 출판사, 디자이너 이름, ISBN으로 검색 결과를 반환
 * - 8.2: 나라, 언어, 연도, 출판사, 장르별로 결과를 필터링
 * - 8.3: 검색 결과를 좋아요 수, 등록일, 연도순으로 정렬
 * - 8.5: 국가별 브라우징
 * - 8.6: 연도별 브라우징
 * - 8.7: 장르별 브라우징
 * - 20.6: 태그별 브라우징
 */
public interface SearchService {

    /**
     * 전문 검색을 수행합니다.
     *
     * @param query 검색어
     * @param filters 필터 조건
     * @param pageable 페이징 정보
     * @return 검색 결과
     */
    SearchResult search(String query, SearchFilters filters, Pageable pageable);

    /**
     * 공개된 원서·번역서 대표 표지와 리디자인을 최신순으로 조회합니다.
     *
     * Requirements: 8.3 - 등록일순 정렬
     */
    Page<BookCoverDocument> browseLatest(Pageable pageable);

    /**
     * 국가별 브라우징을 수행합니다.
     *
     * @param countryCode 국가 코드 (ISO 3166-1)
     * @param pageable 페이징 정보
     * @return 북커버 문서 페이지
     */
    Page<BookCoverDocument> browseByCountry(String countryCode, Pageable pageable);

    /**
     * 연도별 브라우징을 수행합니다.
     *
     * @param year 출판 연도
     * @param pageable 페이징 정보
     * @return 북커버 문서 페이지
     */
    Page<BookCoverDocument> browseByYear(int year, Pageable pageable);

    /**
     * 장르별 브라우징을 수행합니다.
     *
     * @param genre 장르
     * @param pageable 페이징 정보
     * @return 북커버 문서 페이지
     */
    Page<BookCoverDocument> browseByGenre(String genre, Pageable pageable);

    /**
     * 태그별 브라우징을 수행합니다.
     *
     * @param tagId 태그 ID
     * @param pageable 페이징 정보
     * @return 북커버 문서 페이지
     */
    Page<BookCoverDocument> browseByTag(UUID tagId, Pageable pageable);

    /**
     * 전체 인덱스를 재구축합니다.
     */
    void reindexAll();

    /**
     * 특정 원서를 인덱싱합니다.
     *
     * @param bookId 원서 ID
     */
    void indexOriginalBook(UUID bookId);

    /**
     * 특정 번역서를 인덱싱합니다.
     *
     * @param bookId 번역서 ID
     */
    void indexTranslatedBook(UUID bookId);

    /**
     * 특정 리디자인을 인덱싱합니다.
     *
     * @param redesignId 리디자인 ID
     */
    void indexRedesign(UUID redesignId);

    /**
     * 인덱스에서 문서를 제거합니다.
     *
     * @param documentId 문서 ID
     */
    void removeFromIndex(String documentId);

    /**
     * 국가별 북커버 수를 조회합니다.
     *
     * @param countryCode 국가 코드
     * @return 북커버 수
     */
    long countByCountry(String countryCode);

    /**
     * 연도별 북커버 수를 조회합니다.
     *
     * @param year 연도
     * @return 북커버 수
     */
    long countByYear(int year);

    /**
     * 장르별 북커버 수를 조회합니다.
     *
     * @param genre 장르
     * @return 북커버 수
     */
    long countByGenre(String genre);
}
