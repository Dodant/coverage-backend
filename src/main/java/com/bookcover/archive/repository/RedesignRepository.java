package com.bookcover.archive.repository;

import com.bookcover.archive.domain.book.Redesign;
import com.bookcover.archive.domain.common.ContentStatus;
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
 * 리디자인 Repository.
 * 리디자인 엔티티에 대한 데이터 접근을 제공합니다.
 *
 * Requirements:
 * - 3.4: 각 원서/번역서에 연결된 모든 리디자인 목록을 조회할 수 있게 한다
 */
@Repository
public interface RedesignRepository extends JpaRepository<Redesign, UUID> {

    Optional<Redesign> findByImageUrl(String imageUrl);
    Optional<Redesign> findByThumbnailUrl(String thumbnailUrl);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Redesign r WHERE r.id = :id")
    Optional<Redesign> findByIdForUpdate(@Param("id") UUID id);

    /**
     * 원서 ID로 리디자인 목록을 조회합니다.
     *
     * @param originalBookId 원서 ID
     * @return 리디자인 목록
     */
    List<Redesign> findByOriginalBookId(UUID originalBookId);

    /**
     * 원서 ID로 리디자인 목록을 페이징하여 조회합니다.
     *
     * @param originalBookId 원서 ID
     * @param pageable 페이징 정보
     * @return 리디자인 페이지
     */
    Page<Redesign> findByOriginalBookId(UUID originalBookId, Pageable pageable);

    /**
     * 번역서 ID로 리디자인 목록을 조회합니다.
     *
     * @param translatedBookId 번역서 ID
     * @return 리디자인 목록
     */
    List<Redesign> findByTranslatedBookId(UUID translatedBookId);

    /**
     * 번역서 ID로 리디자인 목록을 페이징하여 조회합니다.
     *
     * @param translatedBookId 번역서 ID
     * @param pageable 페이징 정보
     * @return 리디자인 페이지
     */
    Page<Redesign> findByTranslatedBookId(UUID translatedBookId, Pageable pageable);

    /**
     * 회원 ID로 리디자인 목록을 조회합니다.
     *
     * @param userId 회원 ID
     * @return 리디자인 목록
     */
    List<Redesign> findByCreatedById(UUID userId);

    /**
     * 회원 ID로 리디자인 목록을 페이징하여 조회합니다.
     *
     * @param userId 회원 ID
     * @param pageable 페이징 정보
     * @return 리디자인 페이지
     */
    Page<Redesign> findByCreatedById(UUID userId, Pageable pageable);

    /**
     * 상태로 리디자인 목록을 조회합니다.
     *
     * @param status 콘텐츠 상태
     * @param pageable 페이징 정보
     * @return 리디자인 페이지
     */
    Page<Redesign> findByStatus(ContentStatus status, Pageable pageable);

    /**
     * 원서 ID와 상태로 리디자인 목록을 조회합니다.
     *
     * @param originalBookId 원서 ID
     * @param status 콘텐츠 상태
     * @param pageable 페이징 정보
     * @return 리디자인 페이지
     */
    Page<Redesign> findByOriginalBookIdAndStatus(UUID originalBookId, ContentStatus status, Pageable pageable);

    /**
     * 번역서 ID와 상태로 리디자인 목록을 조회합니다.
     *
     * @param translatedBookId 번역서 ID
     * @param status 콘텐츠 상태
     * @param pageable 페이징 정보
     * @return 리디자인 페이지
     */
    Page<Redesign> findByTranslatedBookIdAndStatus(UUID translatedBookId, ContentStatus status, Pageable pageable);

    /**
     * 회원 ID로 리디자인 수를 조회합니다.
     *
     * @param userId 회원 ID
     * @return 리디자인 수
     */
    long countByCreatedById(UUID userId);

    /**
     * 원서 ID로 리디자인 수를 조회합니다.
     *
     * @param originalBookId 원서 ID
     * @return 리디자인 수
     */
    long countByOriginalBookId(UUID originalBookId);

    /**
     * 번역서 ID로 리디자인 수를 조회합니다.
     *
     * @param translatedBookId 번역서 ID
     * @return 리디자인 수
     */
    long countByTranslatedBookId(UUID translatedBookId);

    /**
     * 좋아요 수 기준으로 정렬된 리디자인 목록을 조회합니다.
     *
     * @param pageable 페이징 정보
     * @return 리디자인 페이지
     */
    Page<Redesign> findByStatusOrderByLikeCountDesc(ContentStatus status, Pageable pageable);

    /**
     * 좋아요 수를 1 증가시킵니다 (단일 UPDATE 쿼리).
     *
     * @param id 리디자인 ID
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE Redesign r SET r.likeCount = r.likeCount + 1 WHERE r.id = :id")
    int incrementLikeCount(@Param("id") UUID id);

    /**
     * 좋아요 수를 1 감소시킵니다 (단일 UPDATE 쿼리, 0 미만 방지).
     *
     * @param id 리디자인 ID
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE Redesign r SET r.likeCount = r.likeCount - 1 WHERE r.id = :id AND r.likeCount > 0")
    int decrementLikeCount(@Param("id") UUID id);

    /**
     * 댓글 수를 1 증가시킵니다 (단일 UPDATE 쿼리).
     *
     * @param id 리디자인 ID
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE Redesign r SET r.commentCount = r.commentCount + 1 WHERE r.id = :id")
    int incrementCommentCount(@Param("id") UUID id);

    /**
     * 댓글 수를 1 감소시킵니다 (단일 UPDATE 쿼리, 0 미만 방지).
     *
     * @param id 리디자인 ID
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE Redesign r SET r.commentCount = r.commentCount - 1 WHERE r.id = :id AND r.commentCount > 0")
    int decrementCommentCount(@Param("id") UUID id);
}
