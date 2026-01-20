package com.bookcover.archive.service.impl;

import com.bookcover.archive.domain.common.ContentStatus;
import com.bookcover.archive.domain.social.Comment;
import com.bookcover.archive.domain.social.CommentTargetType;
import com.bookcover.archive.domain.social.Like;
import com.bookcover.archive.domain.social.LikeTargetType;
import com.bookcover.archive.domain.user.User;
import com.bookcover.archive.exception.ResourceNotFoundException;
import com.bookcover.archive.exception.ValidationException;
import com.bookcover.archive.repository.BookCoverRepository;
import com.bookcover.archive.repository.CommentRepository;
import com.bookcover.archive.repository.LikeRepository;
import com.bookcover.archive.repository.RedesignRepository;
import com.bookcover.archive.repository.UserRepository;
import com.bookcover.archive.service.SocialService;
import com.bookcover.archive.service.dto.CreateCommentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 소셜 기능 서비스 구현체.
 * 좋아요 및 댓글 기능을 구현합니다.
 */
@Service
@Transactional
public class SocialServiceImpl implements SocialService {

    private static final int MAX_COMMENT_LENGTH = 1000;

    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final BookCoverRepository bookCoverRepository;
    private final RedesignRepository redesignRepository;

    public SocialServiceImpl(
            LikeRepository likeRepository,
            CommentRepository commentRepository,
            UserRepository userRepository,
            BookCoverRepository bookCoverRepository,
            RedesignRepository redesignRepository) {
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.bookCoverRepository = bookCoverRepository;
        this.redesignRepository = redesignRepository;
    }

    // 좋아요 기능 구현

    @Override
    public boolean toggleLike(UUID userId, LikeTargetType targetType, UUID targetId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // 문제 10 해결: 대상 존재 여부 검증
        validateLikeTargetExists(targetType, targetId);

        Optional<Like> existingLike = likeRepository.findByUserIdAndTargetTypeAndTargetId(
                userId, targetType, targetId);

        if (existingLike.isPresent()) {
            // 좋아요 취소
            likeRepository.delete(existingLike.get());
            decrementLikeCount(targetType, targetId);
            return false;
        } else {
            // 좋아요 추가
            Like like = new Like();
            like.setUser(user);
            like.setTargetType(targetType);
            like.setTargetId(targetId);
            like.setCreatedAt(Instant.now());
            likeRepository.save(like);
            incrementLikeCount(targetType, targetId);
            return true;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int getLikeCount(LikeTargetType targetType, UUID targetId) {
        return (int) likeRepository.countByTargetTypeAndTargetId(targetType, targetId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLikedByUser(UUID userId, LikeTargetType targetType, UUID targetId) {
        return likeRepository.existsByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId);
    }

    // 댓글 기능 구현

    @Override
    public Comment createComment(CreateCommentRequest request) {
        // 공백 검증
        validateCommentContent(request.content());

        User author = userRepository.findById(request.authorId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.authorId()));

        // 문제 10 해결: 대상 존재 여부 검증
        validateCommentTargetExists(request.targetType(), request.targetId());

        Comment comment = new Comment();
        comment.setAuthor(author);
        comment.setTargetType(request.targetType());
        comment.setTargetId(request.targetId());
        comment.setContent(request.content());
        comment.setStatus(ContentStatus.PUBLISHED);
        comment.setCreatedAt(Instant.now());
        comment.setUpdatedAt(Instant.now());

        Comment savedComment = commentRepository.save(comment);

        // 댓글 수 증가
        incrementCommentCount(request.targetType(), request.targetId());

        return savedComment;
    }

    @Override
    public Comment updateComment(UUID commentId, UUID userId, String content) {
        // 공백 검증
        validateCommentContent(content);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        // 작성자 확인
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ValidationException("Only the author can update this comment",
                    Map.of("commentId", commentId, "userId", userId));
        }

        comment.setContent(content);
        comment.setUpdatedAt(Instant.now());

        return commentRepository.save(comment);
    }

    @Override
    public void deleteComment(UUID commentId, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        // 작성자 확인
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ValidationException("Only the author can delete this comment",
                    Map.of("commentId", commentId, "userId", userId));
        }

        CommentTargetType targetType = comment.getTargetType();
        UUID targetId = comment.getTargetId();

        // 댓글 삭제
        commentRepository.delete(comment);

        // 댓글 수 감소
        decrementCommentCount(targetType, targetId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Comment> getComments(CommentTargetType targetType, UUID targetId, Pageable pageable) {
        return commentRepository.findByTargetTypeAndTargetIdAndStatus(
                targetType, targetId, ContentStatus.PUBLISHED, pageable);
    }

    // Private helper methods

    private void validateCommentContent(String content) {
        if (content == null || content.isBlank()) {
            throw new ValidationException("Comment content cannot be empty or blank",
                    Map.of("content", content != null ? content : "null"));
        }
        // 문제 6 해결: 댓글 길이 검증 추가
        if (content.length() > MAX_COMMENT_LENGTH) {
            throw new ValidationException("Comment content exceeds maximum length of " + MAX_COMMENT_LENGTH + " characters",
                    Map.of("contentLength", content.length(), "maxLength", MAX_COMMENT_LENGTH));
        }
    }

    // 문제 1 해결: 단일 UPDATE 쿼리로 변경
    private void incrementLikeCount(LikeTargetType targetType, UUID targetId) {
        switch (targetType) {
            case ORIGINAL_COVER, TRANSLATED_COVER -> bookCoverRepository.incrementLikeCount(targetId);
            case REDESIGN -> redesignRepository.incrementLikeCount(targetId);
        }
    }

    private void decrementLikeCount(LikeTargetType targetType, UUID targetId) {
        switch (targetType) {
            case ORIGINAL_COVER, TRANSLATED_COVER -> bookCoverRepository.decrementLikeCount(targetId);
            case REDESIGN -> redesignRepository.decrementLikeCount(targetId);
        }
    }

    private void incrementCommentCount(CommentTargetType targetType, UUID targetId) {
        switch (targetType) {
            case ORIGINAL_COVER, TRANSLATED_COVER -> bookCoverRepository.incrementCommentCount(targetId);
            case REDESIGN -> redesignRepository.incrementCommentCount(targetId);
        }
    }

    private void decrementCommentCount(CommentTargetType targetType, UUID targetId) {
        switch (targetType) {
            case ORIGINAL_COVER, TRANSLATED_COVER -> bookCoverRepository.decrementCommentCount(targetId);
            case REDESIGN -> redesignRepository.decrementCommentCount(targetId);
        }
    }

    // 문제 10 해결: 좋아요 대상 존재 여부 검증
    private void validateLikeTargetExists(LikeTargetType targetType, UUID targetId) {
        boolean exists = switch (targetType) {
            case ORIGINAL_COVER, TRANSLATED_COVER -> bookCoverRepository.existsById(targetId);
            case REDESIGN -> redesignRepository.existsById(targetId);
        };
        if (!exists) {
            throw new ResourceNotFoundException(targetType.name(), targetId);
        }
    }

    // 문제 10 해결: 댓글 대상 존재 여부 검증
    private void validateCommentTargetExists(CommentTargetType targetType, UUID targetId) {
        boolean exists = switch (targetType) {
            case ORIGINAL_COVER, TRANSLATED_COVER -> bookCoverRepository.existsById(targetId);
            case REDESIGN -> redesignRepository.existsById(targetId);
        };
        if (!exists) {
            throw new ResourceNotFoundException(targetType.name(), targetId);
        }
    }
}
