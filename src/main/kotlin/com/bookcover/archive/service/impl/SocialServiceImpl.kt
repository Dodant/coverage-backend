package com.bookcover.archive.service.impl

import com.bookcover.archive.domain.common.ContentStatus
import com.bookcover.archive.domain.social.Comment
import com.bookcover.archive.domain.social.CommentTargetType
import com.bookcover.archive.domain.social.Like
import com.bookcover.archive.domain.social.LikeTargetType
import com.bookcover.archive.exception.ResourceNotFoundException
import com.bookcover.archive.exception.UnauthorizedException
import com.bookcover.archive.exception.ValidationException
import com.bookcover.archive.repository.social.CommentRepository
import com.bookcover.archive.repository.social.LikeRepository
import com.bookcover.archive.service.SocialService
import com.bookcover.archive.service.dto.CreateCommentRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Implementation of SocialService for likes and comments.
 *
 * Requirements:
 * - 4.1: Record likes and increment like count
 * - 4.2: Toggle like (add/remove)
 * - 4.4: Display like count with covers
 * - 5.1: Store comment content, author, and creation time
 * - 5.2: Reject blank/whitespace-only comments
 * - 5.4: Delete comments
 * - 5.5: Display comments in chronological order
 */
@Service
@Transactional
class SocialServiceImpl(
    private val likeRepository: LikeRepository,
    private val commentRepository: CommentRepository
) : SocialService {

    // ==================== Like Operations ====================

    /**
     * Toggles a like on content.
     * Requirements: 4.1, 4.2
     */
    override fun toggleLike(userId: UUID, targetType: LikeTargetType, targetId: UUID): Boolean {
        val existingLike = likeRepository.findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)

        return if (existingLike != null) {
            // Unlike: remove the existing like
            likeRepository.delete(existingLike)
            false
        } else {
            // Like: create a new like
            val like = Like(
                userId = userId,
                targetType = targetType,
                targetId = targetId
            )
            likeRepository.save(like)
            true
        }
    }

    /**
     * Gets the like count for specific content.
     * Requirement: 4.4
     */
    @Transactional(readOnly = true)
    override fun getLikeCount(targetType: LikeTargetType, targetId: UUID): Int {
        return likeRepository.countByTargetTypeAndTargetId(targetType, targetId).toInt()
    }

    /**
     * Checks if a user has liked specific content.
     */
    @Transactional(readOnly = true)
    override fun isLikedByUser(userId: UUID, targetType: LikeTargetType, targetId: UUID): Boolean {
        return likeRepository.existsByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)
    }

    // ==================== Comment Operations ====================

    /**
     * Creates a new comment on content.
     * Requirements: 5.1, 5.2
     */
    override fun createComment(request: CreateCommentRequest): Comment {
        // Requirement 5.2: Reject blank/whitespace-only comments
        validateCommentContent(request.content)

        val comment = Comment(
            content = request.content.trim(),
            authorId = request.authorId,
            targetType = request.targetType,
            targetId = request.targetId
        )

        return commentRepository.save(comment)
    }

    /**
     * Updates an existing comment.
     */
    override fun updateComment(commentId: UUID, content: String, userId: UUID): Comment {
        val comment = commentRepository.findById(commentId).orElseThrow {
            ResourceNotFoundException("Comment", commentId)
        }

        // Check authorization
        if (comment.authorId != userId) {
            throw UnauthorizedException("You can only edit your own comments")
        }

        // Validate content
        validateCommentContent(content)

        comment.updateContent(content.trim())
        return commentRepository.save(comment)
    }

    /**
     * Deletes a comment.
     * Requirement: 5.4
     */
    override fun deleteComment(commentId: UUID, userId: UUID) {
        val comment = commentRepository.findById(commentId).orElseThrow {
            ResourceNotFoundException("Comment", commentId)
        }

        // Check authorization
        if (comment.authorId != userId) {
            throw UnauthorizedException("You can only delete your own comments")
        }

        // Hard delete the comment as per requirement 5.4
        commentRepository.delete(comment)
    }

    /**
     * Gets comments for specific content.
     * Requirement: 5.5
     */
    @Transactional(readOnly = true)
    override fun getComments(targetType: CommentTargetType, targetId: UUID, pageable: Pageable): Page<Comment> {
        // Only return published comments
        return commentRepository.findByTargetTypeAndTargetIdAndStatus(
            targetType,
            targetId,
            ContentStatus.PUBLISHED,
            pageable
        )
    }

    /**
     * Gets the comment count for specific content.
     */
    @Transactional(readOnly = true)
    override fun getCommentCount(targetType: CommentTargetType, targetId: UUID): Int {
        return commentRepository.countByTargetTypeAndTargetIdAndStatus(
            targetType,
            targetId,
            ContentStatus.PUBLISHED
        ).toInt()
    }

    /**
     * Validates comment content.
     * Requirement 5.2: Reject blank/whitespace-only comments
     */
    private fun validateCommentContent(content: String) {
        if (content.isBlank()) {
            throw ValidationException(
                "Comment content cannot be blank or whitespace-only",
                mapOf("content" to "Comment content is required and cannot be empty or whitespace-only")
            )
        }
    }
}
