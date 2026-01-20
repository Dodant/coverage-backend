package com.bookcover.archive.service

import com.bookcover.archive.domain.social.Comment
import com.bookcover.archive.domain.social.CommentTargetType
import com.bookcover.archive.domain.social.LikeTargetType
import com.bookcover.archive.service.dto.CreateCommentRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

/**
 * Service interface for social features (likes and comments).
 *
 * Requirements:
 * - 4.1, 4.2, 4.4: Like functionality
 * - 5.1, 5.2, 5.4, 5.5: Comment functionality
 */
interface SocialService {

    // ==================== Like Operations ====================

    /**
     * Toggles a like on content. If the user has already liked the content,
     * the like is removed. Otherwise, a new like is created.
     * Requirements: 4.1, 4.2
     *
     * @param userId The ID of the user toggling the like
     * @param targetType The type of content being liked
     * @param targetId The ID of the content being liked
     * @return true if the content is now liked, false if unliked
     */
    fun toggleLike(userId: UUID, targetType: LikeTargetType, targetId: UUID): Boolean

    /**
     * Gets the like count for specific content.
     * Requirement: 4.4
     *
     * @param targetType The type of content
     * @param targetId The ID of the content
     * @return The number of likes
     */
    fun getLikeCount(targetType: LikeTargetType, targetId: UUID): Int

    /**
     * Checks if a user has liked specific content.
     *
     * @param userId The ID of the user
     * @param targetType The type of content
     * @param targetId The ID of the content
     * @return true if the user has liked the content
     */
    fun isLikedByUser(userId: UUID, targetType: LikeTargetType, targetId: UUID): Boolean

    // ==================== Comment Operations ====================

    /**
     * Creates a new comment on content.
     * Requirements: 5.1, 5.2
     *
     * @param request The comment creation request
     * @return The created comment
     * @throws ValidationException if the comment content is blank or whitespace-only
     */
    fun createComment(request: CreateCommentRequest): Comment

    /**
     * Updates an existing comment.
     *
     * @param commentId The ID of the comment to update
     * @param content The new content
     * @param userId The ID of the user making the update (for authorization)
     * @return The updated comment
     * @throws ResourceNotFoundException if comment not found
     * @throws UnauthorizedException if user is not the comment author
     * @throws ValidationException if the content is blank
     */
    fun updateComment(commentId: UUID, content: String, userId: UUID): Comment

    /**
     * Deletes a comment.
     * Requirement: 5.4
     *
     * @param commentId The ID of the comment to delete
     * @param userId The ID of the user making the deletion (for authorization)
     * @throws ResourceNotFoundException if comment not found
     * @throws UnauthorizedException if user is not the comment author
     */
    fun deleteComment(commentId: UUID, userId: UUID)

    /**
     * Gets comments for specific content.
     * Requirement: 5.5
     *
     * @param targetType The type of content
     * @param targetId The ID of the content
     * @param pageable Pagination information
     * @return Page of comments in chronological order
     */
    fun getComments(targetType: CommentTargetType, targetId: UUID, pageable: Pageable): Page<Comment>

    /**
     * Gets the comment count for specific content.
     *
     * @param targetType The type of content
     * @param targetId The ID of the content
     * @return The number of comments
     */
    fun getCommentCount(targetType: CommentTargetType, targetId: UUID): Int
}
