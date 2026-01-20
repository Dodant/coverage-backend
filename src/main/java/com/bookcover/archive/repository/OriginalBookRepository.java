package com.bookcover.archive.repository;

import com.bookcover.archive.domain.book.OriginalBook;
import com.bookcover.archive.domain.common.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 원서 Repository.
 * 원서 엔티티에 대한 데이터 접근을 제공합니다.
 *
 * Requirements:
 * - 1.5: 원서가 성공적으로 등록되면 고유 식별자를 생성하고 데이터베이스에 저장
 */
@Repository
public interface OriginalBookRepository extends JpaRepository<OriginalBook, UUID> {

    /**
     * ISBN으로 원서를 조회합니다.
     *
     * @param isbn ISBN
     * @return 원서 Optional
     */
    Optional<OriginalBook> findByIsbn(String isbn);

    /**
     * ISBN이 존재하는지 확인합니다.
     *
     * @param isbn ISBN
     * @return 존재 여부
     */
    boolean existsByIsbn(String isbn);

    /**
     * 작가 ID로 원서 목록을 조회합니다.
     *
     * @param authorId 작가 ID
     * @param pageable 페이징 정보
     * @return 원서 페이지
     */
    Page<OriginalBook> findByAuthorId(UUID authorId, Pageable pageable);

    /**
     * 출판사 ID로 원서 목록을 조회합니다.
     *
     * @param publisherId 출판사 ID
     * @param pageable 페이징 정보
     * @return 원서 페이지
     */
    Page<OriginalBook> findByPublisherId(UUID publisherId, Pageable pageable);

    /**
     * 디자이너 ID로 원서 목록을 조회합니다.
     *
     * @param designerId 디자이너 ID
     * @param pageable 페이징 정보
     * @return 원서 페이지
     */
    Page<OriginalBook> findByDesignerId(UUID designerId, Pageable pageable);

    /**
     * 작품(Work) ID로 원서 목록을 조회합니다.
     *
     * @param workId 작품 ID
     * @return 원서 목록
     */
    List<OriginalBook> findByWorkId(UUID workId);

    /**
     * 국가 코드로 원서 목록을 조회합니다.
     *
     * @param countryCode ISO 3166-1 국가 코드
     * @param pageable 페이징 정보
     * @return 원서 페이지
     */
    Page<OriginalBook> findByCountryCode(String countryCode, Pageable pageable);

    /**
     * 연도로 원서 목록을 조회합니다.
     *
     * @param year 출판 연도
     * @param pageable 페이징 정보
     * @return 원서 페이지
     */
    Page<OriginalBook> findByYear(Integer year, Pageable pageable);

    /**
     * 장르로 원서 목록을 조회합니다.
     *
     * @param genre 장르
     * @param pageable 페이징 정보
     * @return 원서 페이지
     */
    Page<OriginalBook> findByGenre(String genre, Pageable pageable);

    /**
     * 상태로 원서 목록을 조회합니다.
     *
     * @param status 콘텐츠 상태
     * @param pageable 페이징 정보
     * @return 원서 페이지
     */
    Page<OriginalBook> findByStatus(ContentStatus status, Pageable pageable);

    /**
     * 등록자 ID로 원서 목록을 조회합니다.
     *
     * @param userId 등록자 ID
     * @param pageable 페이징 정보
     * @return 원서 페이지
     */
    Page<OriginalBook> findByCreatedById(UUID userId, Pageable pageable);

    /**
     * 등록자 ID로 원서 수를 조회합니다.
     *
     * @param userId 등록자 ID
     * @return 원서 수
     */
    long countByCreatedById(UUID userId);

    /**
     * 제목으로 원서를 검색합니다 (대소문자 무시).
     *
     * @param title 제목 검색어
     * @param pageable 페이징 정보
     * @return 원서 페이지
     */
    Page<OriginalBook> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    /**
     * 원서 제목으로 원서를 검색합니다 (대소문자 무시).
     *
     * @param originalTitle 원서 제목 검색어
     * @param pageable 페이징 정보
     * @return 원서 페이지
     */
    Page<OriginalBook> findByOriginalTitleContainingIgnoreCase(String originalTitle, Pageable pageable);
}
