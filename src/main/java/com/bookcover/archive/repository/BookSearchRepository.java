package com.bookcover.archive.repository;

import com.bookcover.archive.domain.media.Media;

import java.util.List;

/**
 * 책 검색을 위한 커스텀 Repository 인터페이스.
 * 중복 검사 및 유사 항목 검색 쿼리를 제공합니다.
 *
 * Requirements:
 * - 12.1: 동일한 ISBN이 이미 존재하면 중복 경고를 표시하고 기존 엔트리 정보를 보여준다
 * - 12.2: 제목, 저자, 연도가 유사한 책이 존재하면 유사 항목 목록을 표시한다
 */
public interface BookSearchRepository {

    /**
     * ISBN으로 중복 책을 검색합니다.
     *
     * @param isbn ISBN
     * @return 중복 책 목록
     */
    List<Media> findDuplicatesByIsbn(String isbn);

    /**
     * 제목, 작가 이름, 연도로 유사한 책을 검색합니다.
     *
     * @param title 제목
     * @param authorName 작가 이름
     * @param year 출판 연도 (null이면 연도 조건 없이, 연도를 모르는 책은 항상 포함)
     * @return 유사 책 목록
     */
    List<Media> findSimilarBooks(String title, String authorName, Integer year);

    /**
     * 제목으로 유사한 책을 검색합니다.
     *
     * @param title 제목
     * @return 유사 책 목록
     */
    List<Media> findByTitleSimilar(String title);
}
