package com.bookcover.archive.repository;

import com.bookcover.archive.domain.book.BookCover;
import com.bookcover.archive.domain.book.CoverLayout;
import com.bookcover.archive.domain.book.CoverType;
import com.bookcover.archive.domain.book.CreditRole;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 북커버 Repository.
 * 북커버 엔티티에 대한 데이터 접근을 제공합니다.
 */
@Repository
public interface BookCoverRepository extends JpaRepository<BookCover, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM BookCover c WHERE c.id = :id")
    Optional<BookCover> findByIdForUpdate(@Param("id") UUID id);

    /**
     * 커버 유형으로 북커버 목록을 조회합니다.
     *
     * @param coverType 커버 유형
     * @param pageable 페이징 정보
     * @return 북커버 페이지
     */
    Page<BookCover> findByCoverType(CoverType coverType, Pageable pageable);

    /**
     * 레이아웃으로 북커버 목록을 조회합니다.
     *
     * @param layout 레이아웃
     * @param pageable 페이징 정보
     * @return 북커버 페이지
     */
    Page<BookCover> findByLayout(CoverLayout layout, Pageable pageable);

    /**
     * 대표 색상으로 북커버 목록을 조회합니다.
     *
     * @param dominantColor 대표 색상 (HEX)
     * @param pageable 페이징 정보
     * @return 북커버 페이지
     */
    Page<BookCover> findByDominantColor(String dominantColor, Pageable pageable);

    /**
     * 좋아요 수 기준으로 정렬된 북커버 목록을 조회합니다.
     *
     * @param pageable 페이징 정보
     * @return 북커버 페이지
     */
    Page<BookCover> findAllByOrderByLikeCountDesc(Pageable pageable);

    /**
     * 이미지 URL로 북커버를 조회합니다.
     *
     * @param imageUrl 이미지 URL
     * @return 북커버 목록
     */
    List<BookCover> findByImageUrl(String imageUrl);
    List<BookCover> findByThumbnailUrl(String thumbnailUrl);

    /** 비슷한 표지 묶음의 표지들 */
    List<BookCover> findBySimilarGroupId(UUID similarGroupId);

    /**
     * 디자이너가 크레딧에 오른 표지 목록 (앞면 변형·뒷면 모두, 공개(PUBLISHED) 책의 표지만, 최근 등록순).
     * role이 null이면 역할 무관.
     */
    @Query(value = "SELECT c FROM BookCover c " +
        "WHERE EXISTS (SELECT 1 FROM CoverCredit cr WHERE cr.cover = c AND cr.designer.id = :designerId " +
        "AND (:role IS NULL OR cr.role = :role)) " +
        "AND (EXISTS (SELECT 1 FROM Media m WHERE m.id = c.book.id AND m.status = com.bookcover.archive.domain.common.ContentStatus.PUBLISHED) " +
        "OR EXISTS (SELECT 1 FROM OriginalBook ob WHERE ob.backCover = c AND ob.status = com.bookcover.archive.domain.common.ContentStatus.PUBLISHED) " +
        "OR EXISTS (SELECT 1 FROM TranslatedBook tb WHERE tb.backCover = c AND tb.status = com.bookcover.archive.domain.common.ContentStatus.PUBLISHED))" +
        " ORDER BY c.createdAt DESC",
        countQuery = "SELECT count(c) FROM BookCover c " +
        "WHERE EXISTS (SELECT 1 FROM CoverCredit cr WHERE cr.cover = c AND cr.designer.id = :designerId " +
        "AND (:role IS NULL OR cr.role = :role)) " +
        "AND (EXISTS (SELECT 1 FROM Media m WHERE m.id = c.book.id AND m.status = com.bookcover.archive.domain.common.ContentStatus.PUBLISHED) " +
        "OR EXISTS (SELECT 1 FROM OriginalBook ob WHERE ob.backCover = c AND ob.status = com.bookcover.archive.domain.common.ContentStatus.PUBLISHED) " +
        "OR EXISTS (SELECT 1 FROM TranslatedBook tb WHERE tb.backCover = c AND tb.status = com.bookcover.archive.domain.common.ContentStatus.PUBLISHED))")
    Page<BookCover> findCreditedCovers(@Param("designerId") UUID designerId, @Param("role") CreditRole role, Pageable pageable);

    /**
     * 좋아요 수를 1 증가시킵니다 (단일 UPDATE 쿼리).
     *
     * @param id 북커버 ID
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE BookCover b SET b.likeCount = b.likeCount + 1 WHERE b.id = :id")
    int incrementLikeCount(@Param("id") UUID id);

    /**
     * 좋아요 수를 1 감소시킵니다 (단일 UPDATE 쿼리, 0 미만 방지).
     *
     * @param id 북커버 ID
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE BookCover b SET b.likeCount = b.likeCount - 1 WHERE b.id = :id AND b.likeCount > 0")
    int decrementLikeCount(@Param("id") UUID id);

    /**
     * 댓글 수를 1 증가시킵니다 (단일 UPDATE 쿼리).
     *
     * @param id 북커버 ID
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE BookCover b SET b.commentCount = b.commentCount + 1 WHERE b.id = :id")
    int incrementCommentCount(@Param("id") UUID id);

    /**
     * 댓글 수를 1 감소시킵니다 (단일 UPDATE 쿼리, 0 미만 방지).
     *
     * @param id 북커버 ID
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE BookCover b SET b.commentCount = b.commentCount - 1 WHERE b.id = :id AND b.commentCount > 0")
    int decrementCommentCount(@Param("id") UUID id);
}
