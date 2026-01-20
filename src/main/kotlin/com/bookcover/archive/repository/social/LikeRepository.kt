package com.bookcover.archive.repository.social

import com.bookcover.archive.domain.social.Like
import com.bookcover.archive.domain.social.LikeTargetType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for Like entity operations.
 *
 * Requirements:
 * - 4.1: Record likes and increment like count
 * - 4.2: Toggle like (add/remove)
 * - 4.3: Independent like functionality for different content types
 */
@Repository
interface LikeRepository : JpaRepository<Like, UUID> {

    /**
     * Finds a like by user, target type, and target ID.
     * Used for checking if a user has already liked content.
     * @param userId The user's UUID
     * @param targetType The type of content
     * @param targetId The content's UUID
     * @return The like if found
     */
    fun findByUserIdAndTargetTypeAndTargetId(
        userId: UUID,
        targetType: LikeTargetType,
        targetId: UUID
    ): Like?

    /**
     * Checks if a user has liked specific content.
     * @param userId The user's UUID
     * @param targetType The type of content
     * @param targetId The content's UUID
     * @return true if the user has liked the content
     */
    fun existsByUserIdAndTargetTypeAndTargetId(
        userId: UUID,
        targetType: LikeTargetType,
        targetId: UUID
    ): Boolean

    /**
     * Counts likes for specific content.
     * @param targetType The type of content
     * @param targetId The content's UUID
     * @return Count of likes
     */
    fun countByTargetTypeAndTargetId(targetType: LikeTargetType, targetId: UUID): Long

    /**
     * Finds all likes by a user.
     * @param userId The user's UUID
     * @param pageable Pagination information
     * @return Page of likes by the user
     */
    fun findByUserId(userId: UUID, pageable: Pageable): Page<Like>

    /**
     * Finds all likes by a user for a specific content type.
     * @param userId The user's UUID
     * @param targetType The type of content
     * @param pageable Pagination information
     * @return Page of likes
     */
    fun findByUserIdAndTargetType(userId: UUID, targetType: LikeTargetType, pageable: Pageable): Page<Like>

    /**
     * Finds all likes for specific content.
     * @param targetType The type of content
     * @param targetId The content's UUID
     * @param pageable Pagination information
     * @return Page of likes
     */
    fun findByTargetTypeAndTargetId(targetType: LikeTargetType, targetId: UUID, pageable: Pageable): Page<Like>

    /**
     * Deletes a like by user, target type, and target ID.
     * Used for unlike functionality.
     * @param userId The user's UUID
     * @param targetType The type of content
     * @param targetId The content's UUID
     */
    fun deleteByUserIdAndTargetTypeAndTargetId(userId: UUID, targetType: LikeTargetType, targetId: UUID)

    /**
     * Deletes all likes for specific content.
     * Used when content is deleted.
     * @param targetType The type of content
     * @param targetId The content's UUID
     */
    fun deleteByTargetTypeAndTargetId(targetType: LikeTargetType, targetId: UUID)

    /**
     * Counts likes by a user.
     * @param userId The user's UUID
     * @return Count of likes by the user
     */
    fun countByUserId(userId: UUID): Long
}
