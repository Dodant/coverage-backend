package com.bookcover.archive.repository.social

import com.bookcover.archive.domain.common.ContentStatus
import com.bookcover.archive.domain.social.Comment
import com.bookcover.archive.domain.social.CommentTargetType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for Comment entity operations.
 *
 * Requirements:
 * - 5.1: Store comment content, author, and creation time
 * - 5.3: Independent comment functionality for different content types
 * - 5.4: Comment deletion
 * - 5.5: Display comments in chronological order
 */
@Repository
interface CommentRepository : JpaRepository<Comment, UUID> {

    /**
     * Finds all comments for specific content.
     * Requirement 5.5: Display comments in chronological order
     * @param targetType The type of content
     * @param targetId The content's UUID
     * @param pageable Pagination information
     * @return Page of comments
     */
    fun findByTargetTypeAndTargetId(
        targetType: CommentTargetType,
        targetId: UUID,
        pageable: Pageable
    ): Page<Comment>

    /**
     * Finds all published comments for specific content.
     * @param targetType The type of content
     * @param targetId The content's UUID
     * @param status The content status
     * @param pageable Pagination information
     * @return Page of comments
     */
    fun findByTargetTypeAndTargetIdAndStatus(
        targetType: CommentTargetType,
        targetId: UUID,
        status: ContentStatus,
        pageable: Pageable
    ): Page<Comment>

    /**
     * Finds all comments by an author.
     * @param authorId The author's UUID
     * @param pageable Pagination information
     * @return Page of comments
     */
    fun findByAuthorId(authorId: UUID, pageable: Pageable): Page<Comment>

    /**
     * Finds all comments by an author with specific status.
     * @param authorId The author's UUID
     * @param status The content status
     * @param pageable Pagination information
     * @return Page of comments
     */
    fun findByAuthorIdAndStatus(authorId: UUID, status: ContentStatus, pageable: Pageable): Page<Comment>

    /**
     * Counts comments for specific content.
     * @param targetType The type of content
     * @param targetId The content's UUID
     * @return Count of comments
     */
    fun countByTargetTypeAndTargetId(targetType: CommentTargetType, targetId: UUID): Long

    /**
     * Counts published comments for specific content.
     * @param targetType The type of content
     * @param targetId The content's UUID
     * @param status The content status
     * @return Count of comments
     */
    fun countByTargetTypeAndTargetIdAndStatus(
        targetType: CommentTargetType,
        targetId: UUID,
        status: ContentStatus
    ): Long

    /**
     * Counts comments by an author.
     * @param authorId The author's UUID
     * @return Count of comments
     */
    fun countByAuthorId(authorId: UUID): Long

    /**
     * Deletes all comments for specific content.
     * Used when content is deleted.
     * @param targetType The type of content
     * @param targetId The content's UUID
     */
    fun deleteByTargetTypeAndTargetId(targetType: CommentTargetType, targetId: UUID)

    /**
     * Finds comments by status.
     * @param status The content status
     * @param pageable Pagination information
     * @return Page of comments
     */
    fun findByStatus(status: ContentStatus, pageable: Pageable): Page<Comment>
}
