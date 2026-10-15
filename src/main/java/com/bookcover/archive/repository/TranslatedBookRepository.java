package com.bookcover.archive.repository;

import com.bookcover.archive.domain.book.TranslatedBook;
import com.bookcover.archive.domain.common.ContentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 번역서 Repository.
 * 번역서 엔티티에 대한 데이터 접근을 제공합니다.
 *
 * Requirements:
 * - 2.1: 번역서를 등록하면 해당 번역서를 기존 원서와 연결
 */
@Repository
public interface TranslatedBookRepository extends JpaRepository<TranslatedBook, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM TranslatedBook b WHERE b.id = :id")
    Optional<TranslatedBook> findByIdForUpdate(@Param("id") UUID id);

    /** 뒷면 표지로 번역서를 찾습니다 */
    Optional<TranslatedBook> findByBackCoverId(UUID coverId);

    /**
     * ISBN으로 번역서를 조회합니다.
     *
     * @param isbn ISBN
     * @return 번역서 Optional
     */
    Optional<TranslatedBook> findByIsbn(String isbn);

    /**
     * ISBN이 존재하는지 확인합니다.
     *
     * @param isbn ISBN
     * @return 존재 여부
     */
    boolean existsByIsbn(String isbn);

    /**
     * 원서 ID로 번역서 목록을 조회합니다.
     *
     * @param originalBookId 원서 ID
     * @return 번역서 목록
     */
    List<TranslatedBook> findByOriginalBookId(UUID originalBookId);

    /**
     * 원서 ID로 번역서 목록을 페이징하여 조회합니다.
     *
     * @param originalBookId 원서 ID
     * @param pageable 페이징 정보
     * @return 번역서 페이지
     */
    Page<TranslatedBook> findByOriginalBookId(UUID originalBookId, Pageable pageable);

    /**
     * 작가 ID로 번역서 목록을 조회합니다.
     *
     * @param authorId 작가 ID
     * @param pageable 페이징 정보
     * @return 번역서 페이지
     */
    @Query(value = "SELECT tb FROM TranslatedBook tb " +
        "LEFT JOIN FETCH tb.author " +
        "LEFT JOIN FETCH tb.translator " +
        "LEFT JOIN FETCH tb.publisher " +
        "WHERE tb.author.id = :authorId",
        countQuery = "SELECT count(tb) FROM TranslatedBook tb WHERE tb.author.id = :authorId")
    Page<TranslatedBook> findByAuthorId(@Param("authorId") UUID authorId, Pageable pageable);

    /**
     * 번역가 ID로 번역서 목록을 조회합니다.
     *
     * @param translatorId 번역가 ID
     * @param pageable 페이징 정보
     * @return 번역서 페이지
     */
    @Query(value = "SELECT tb FROM TranslatedBook tb " +
        "LEFT JOIN FETCH tb.author " +
        "LEFT JOIN FETCH tb.translator " +
        "LEFT JOIN FETCH tb.publisher " +
        "WHERE tb.translator.id = :translatorId",
        countQuery = "SELECT count(tb) FROM TranslatedBook tb WHERE tb.translator.id = :translatorId")
    Page<TranslatedBook> findByTranslatorId(@Param("translatorId") UUID translatorId, Pageable pageable);

    /**
     * 출판사 ID로 번역서 목록을 조회합니다.
     *
     * @param publisherId 출판사 ID
     * @param pageable 페이징 정보
     * @return 번역서 페이지
     */
    @Query(value = "SELECT tb FROM TranslatedBook tb " +
        "LEFT JOIN FETCH tb.author " +
        "LEFT JOIN FETCH tb.translator " +
        "LEFT JOIN FETCH tb.publisher " +
        "WHERE tb.publisher.id = :publisherId",
        countQuery = "SELECT count(tb) FROM TranslatedBook tb WHERE tb.publisher.id = :publisherId")
    Page<TranslatedBook> findByPublisherId(@Param("publisherId") UUID publisherId, Pageable pageable);


    /**
     * 국가 코드로 번역서 목록을 조회합니다.
     *
     * @param countryCode ISO 3166-1 국가 코드
     * @param pageable 페이징 정보
     * @return 번역서 페이지
     */
    Page<TranslatedBook> findByCountryCode(String countryCode, Pageable pageable);

    /**
     * 언어 코드로 번역서 목록을 조회합니다.
     *
     * @param languageCode ISO 639-1 언어 코드
     * @param pageable 페이징 정보
     * @return 번역서 페이지
     */
    Page<TranslatedBook> findByLanguageCode(String languageCode, Pageable pageable);

    /**
     * 연도로 번역서 목록을 조회합니다.
     *
     * @param year 출판 연도
     * @param pageable 페이징 정보
     * @return 번역서 페이지
     */
    Page<TranslatedBook> findByYear(Integer year, Pageable pageable);

    /**
     * 상태로 번역서 목록을 조회합니다.
     *
     * @param status 콘텐츠 상태
     * @param pageable 페이징 정보
     * @return 번역서 페이지
     */
    Page<TranslatedBook> findByStatus(ContentStatus status, Pageable pageable);

    /**
     * 등록자 ID로 번역서 목록을 조회합니다.
     *
     * @param userId 등록자 ID
     * @param pageable 페이징 정보
     * @return 번역서 페이지
     */
    Page<TranslatedBook> findByCreatedById(UUID userId, Pageable pageable);

    /**
     * 등록자 ID로 번역서 수를 조회합니다.
     *
     * @param userId 등록자 ID
     * @return 번역서 수
     */
    long countByCreatedById(UUID userId);

    /**
     * 제목으로 번역서를 검색합니다 (대소문자 무시).
     *
     * @param title 제목 검색어
     * @param pageable 페이징 정보
     * @return 번역서 페이지
     */
    Page<TranslatedBook> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    /**
     * 원서 ID로 번역서 수를 조회합니다.
     *
     * @param originalBookId 원서 ID
     * @return 번역서 수
     */
    long countByOriginalBookId(UUID originalBookId);
}
