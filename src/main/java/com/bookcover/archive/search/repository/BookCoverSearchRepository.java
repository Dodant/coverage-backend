package com.bookcover.archive.search.repository;

import com.bookcover.archive.search.document.BookCoverDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * Elasticsearch 북커버 검색 Repository.
 * 기본적인 CRUD 및 검색 기능을 제공합니다.
 *
 * Requirements:
 * - 8.1: 책 제목, 작가, 출판사, 디자이너 이름, ISBN으로 검색 결과를 반환
 * - 8.5: 국가별 브라우징
 * - 8.6: 연도별 브라우징
 * - 8.7: 장르별 브라우징
 */
@Repository
public interface BookCoverSearchRepository extends ElasticsearchRepository<BookCoverDocument, String> {

    /**
     * 국가 코드로 북커버를 검색합니다.
     *
     * @param countryCode 국가 코드 (ISO 3166-1)
     * @param status 콘텐츠 상태
     * @param pageable 페이징 정보
     * @return 북커버 문서 페이지
     */
    Page<BookCoverDocument> findByCountryCodeAndStatus(String countryCode, String status, Pageable pageable);

    /**
     * 연도로 북커버를 검색합니다.
     *
     * @param year 출판 연도
     * @param status 콘텐츠 상태
     * @param pageable 페이징 정보
     * @return 북커버 문서 페이지
     */
    Page<BookCoverDocument> findByYearAndStatus(Integer year, String status, Pageable pageable);

    /**
     * 장르로 북커버를 검색합니다.
     *
     * @param genre 장르
     * @param status 콘텐츠 상태
     * @param pageable 페이징 정보
     * @return 북커버 문서 페이지
     */
    Page<BookCoverDocument> findByGenreAndStatus(String genre, String status, Pageable pageable);

    /**
     * 태그 ID로 북커버를 검색합니다.
     *
     * @param tagId 태그 ID
     * @param status 콘텐츠 상태
     * @param pageable 페이징 정보
     * @return 북커버 문서 페이지
     */
    Page<BookCoverDocument> findByTagIdsContainingAndStatus(String tagId, String status, Pageable pageable);

    /**
     * 문서 유형으로 북커버를 검색합니다.
     *
     * @param type 문서 유형 (ORIGINAL, TRANSLATED, REDESIGN)
     * @param status 콘텐츠 상태
     * @param pageable 페이징 정보
     * @return 북커버 문서 페이지
     */
    Page<BookCoverDocument> findByTypeAndStatus(String type, String status, Pageable pageable);

    /**
     * 작가 ID로 북커버를 검색합니다.
     *
     * @param authorId 작가 ID
     * @param status 콘텐츠 상태
     * @param pageable 페이징 정보
     * @return 북커버 문서 페이지
     */
    Page<BookCoverDocument> findByAuthorIdAndStatus(String authorId, String status, Pageable pageable);

    /**
     * 출판사 ID로 북커버를 검색합니다.
     *
     * @param publisherId 출판사 ID
     * @param status 콘텐츠 상태
     * @param pageable 페이징 정보
     * @return 북커버 문서 페이지
     */
    Page<BookCoverDocument> findByPublisherIdAndStatus(String publisherId, String status, Pageable pageable);

    /**
     * 표지 크레딧 기여자 ID로 북커버를 검색합니다.
     *
     * @param designerId 디자이너 ID
     * @param status 콘텐츠 상태
     * @param pageable 페이징 정보
     * @return 북커버 문서 페이지
     */
    Page<BookCoverDocument> findByDesignerIdsAndStatus(String designerId, String status, Pageable pageable);

    /**
     * 상태로 모든 북커버를 검색합니다.
     *
     * @param status 콘텐츠 상태
     * @param pageable 페이징 정보
     * @return 북커버 문서 페이지
     */
    Page<BookCoverDocument> findByStatus(String status, Pageable pageable);

    /**
     * 국가 코드로 북커버 수를 카운트합니다.
     *
     * @param countryCode 국가 코드
     * @param status 콘텐츠 상태
     * @return 북커버 수
     */
    long countByCountryCodeAndStatus(String countryCode, String status);

    /**
     * 연도로 북커버 수를 카운트합니다.
     *
     * @param year 출판 연도
     * @param status 콘텐츠 상태
     * @return 북커버 수
     */
    long countByYearAndStatus(Integer year, String status);

    /**
     * 장르로 북커버 수를 카운트합니다.
     *
     * @param genre 장르
     * @param status 콘텐츠 상태
     * @return 북커버 수
     */
    long countByGenreAndStatus(String genre, String status);
}
