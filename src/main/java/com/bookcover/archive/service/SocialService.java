package com.bookcover.archive.service;

import com.bookcover.archive.domain.social.Comment;
import com.bookcover.archive.domain.social.CommentTargetType;
import com.bookcover.archive.domain.social.LikeTargetType;
import com.bookcover.archive.service.dto.CreateCommentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * 소셜 기능 서비스 인터페이스.
 * 좋아요 및 댓글 기능을 제공합니다.
 *
 * Requirements:
 * - 4.1: 회원이 북커버에 좋아요를 누르면 해당 회원의 좋아요를 기록하고 좋아요 수를 증가시킨다
 * - 4.2: 회원이 이미 좋아요를 누른 북커버에 다시 좋아요를 누르면 좋아요를 취소하고 좋아요 수를 감소시킨다
 * - 4.4: 북커버 목록을 조회하면 각 커버의 좋아요 수를 함께 표시한다
 * - 5.1: 회원이 댓글을 작성하면 댓글 내용, 작성자, 작성 시간을 저장한다
 * - 5.2: 댓글이 빈 문자열이거나 공백만 포함하면 댓글 등록을 거부한다
 * - 5.4: 회원이 자신의 댓글을 삭제하면 해당 댓글을 데이터베이스에서 제거한다
 * - 5.5: 북커버 상세 페이지를 조회하면 해당 커버의 모든 댓글을 시간순으로 표시한다
 */
public interface SocialService {

    // 좋아요 기능

    /**
     * 좋아요를 토글합니다.
     * 좋아요가 없으면 추가하고, 있으면 제거합니다.
     *
     * @param userId 회원 ID
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     * @return 좋아요 추가 시 true, 제거 시 false
     */
    boolean toggleLike(UUID userId, LikeTargetType targetType, UUID targetId);

    /**
     * 대상의 좋아요 수를 조회합니다.
     *
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     * @return 좋아요 수
     */
    int getLikeCount(LikeTargetType targetType, UUID targetId);

    /**
     * 회원이 대상에 좋아요를 눌렀는지 확인합니다.
     *
     * @param userId 회원 ID
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     * @return 좋아요 여부
     */
    boolean isLikedByUser(UUID userId, LikeTargetType targetType, UUID targetId);

    // 댓글 기능

    /**
     * 댓글을 생성합니다.
     *
     * @param request 댓글 생성 요청
     * @return 생성된 댓글
     */
    Comment createComment(CreateCommentRequest request);

    /**
     * 댓글 내용을 수정합니다.
     *
     * @param commentId 댓글 ID
     * @param userId 요청 회원 ID (작성자 확인용)
     * @param content 새 댓글 내용
     * @return 수정된 댓글
     */
    Comment updateComment(UUID commentId, UUID userId, String content);

    /**
     * 댓글을 삭제합니다.
     *
     * @param commentId 댓글 ID
     * @param userId 요청 회원 ID (작성자 확인용)
     */
    void deleteComment(UUID commentId, UUID userId);

    /**
     * 대상의 댓글 목록을 시간순으로 조회합니다.
     *
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     * @param pageable 페이징 정보
     * @return 댓글 페이지
     */
    Page<Comment> getComments(CommentTargetType targetType, UUID targetId, Pageable pageable);
}
