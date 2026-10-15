package com.bookcover.archive.repository;

import com.bookcover.archive.domain.common.ContentStatus;
import com.bookcover.archive.domain.social.Comment;
import com.bookcover.archive.domain.social.CommentTargetType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 댓글 Repository.
 * 댓글 엔티티에 대한 데이터 접근을 제공합니다.
 *
 * Requirements:
 * - 5.1: 회원이 댓글을 작성하면 댓글 내용, 작성자, 작성 시간을 저장한다
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Comment c WHERE c.id = :id")
    Optional<Comment> findByIdForUpdate(@Param("id") UUID id);

    /**
     * 대상 유형과 대상 ID로 댓글 목록을 조회합니다.
     *
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     * @return 댓글 목록
     */
    List<Comment> findByTargetTypeAndTargetId(CommentTargetType targetType, UUID targetId);

    /**
     * 대상 유형과 대상 ID로 댓글 목록을 페이징하여 조회합니다.
     *
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     * @param pageable 페이징 정보
     * @return 댓글 페이지
     */
    Page<Comment> findByTargetTypeAndTargetId(CommentTargetType targetType, UUID targetId, Pageable pageable);

    /**
     * 대상 유형, 대상 ID, 상태로 댓글 목록을 조회합니다.
     *
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     * @param status 콘텐츠 상태
     * @param pageable 페이징 정보
     * @return 댓글 페이지
     */
    Page<Comment> findByTargetTypeAndTargetIdAndStatus(CommentTargetType targetType, UUID targetId, ContentStatus status, Pageable pageable);

    /**
     * 작성자 ID로 댓글 목록을 조회합니다.
     *
     * @param authorId 작성자 ID
     * @param pageable 페이징 정보
     * @return 댓글 페이지
     */
    Page<Comment> findByAuthorId(UUID authorId, Pageable pageable);

    /**
     * 작성자 ID와 상태로 댓글 목록을 조회합니다.
     *
     * @param authorId 작성자 ID
     * @param status 콘텐츠 상태
     * @param pageable 페이징 정보
     * @return 댓글 페이지
     */
    Page<Comment> findByAuthorIdAndStatus(UUID authorId, ContentStatus status, Pageable pageable);

    /**
     * 대상 유형과 대상 ID로 댓글 수를 조회합니다.
     *
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     * @return 댓글 수
     */
    long countByTargetTypeAndTargetId(CommentTargetType targetType, UUID targetId);

    /**
     * 대상 유형, 대상 ID, 상태로 댓글 수를 조회합니다.
     *
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     * @param status 콘텐츠 상태
     * @return 댓글 수
     */
    long countByTargetTypeAndTargetIdAndStatus(CommentTargetType targetType, UUID targetId, ContentStatus status);

    /**
     * 작성자 ID로 댓글 수를 조회합니다.
     *
     * @param authorId 작성자 ID
     * @return 댓글 수
     */
    long countByAuthorId(UUID authorId);

    /**
     * 대상 유형과 대상 ID로 모든 댓글을 삭제합니다.
     *
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     */
    void deleteByTargetTypeAndTargetId(CommentTargetType targetType, UUID targetId);

    /**
     * 특정 기간 내에 작성자가 작성한 댓글 수를 조회합니다 (레이트 리밋용).
     *
     * @param authorId 작성자 ID
     * @param after 시작 시간
     * @return 댓글 수
     */
    long countByAuthorIdAndCreatedAtAfter(UUID authorId, Instant after);

    /**
     * 상태로 댓글 목록을 조회합니다.
     *
     * @param status 콘텐츠 상태
     * @param pageable 페이징 정보
     * @return 댓글 페이지
     */
    Page<Comment> findByStatus(ContentStatus status, Pageable pageable);

    /**
     * 대상 유형과 대상 ID로 시간순 정렬된 댓글 목록을 조회합니다.
     *
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     * @param status 콘텐츠 상태
     * @return 댓글 목록 (시간순)
     */
    List<Comment> findByTargetTypeAndTargetIdAndStatusOrderByCreatedAtAsc(CommentTargetType targetType, UUID targetId, ContentStatus status);
}
