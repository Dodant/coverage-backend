package com.bookcover.archive.domain.social

import com.bookcover.archive.domain.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

/**
 * Represents a like on content (cover or redesign) by a user.
 * Uses polymorphic design to support likes on different content types.
 * Each user can only like a specific piece of content once (enforced by unique constraint).
 *
 * Requirements:
 * - 4.1: Record likes and increment like count
 * - 4.3: Independent like functionality for original covers, translated covers, and redesigns
 */
@Entity
@Table(
    name = "likes",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_likes_user_target",
            columnNames = ["user_id", "target_type", "target_id"]
        )
    ]
)
class Like(
    /** UUID of the user who created this like */
    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    /** Type of content being liked */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    val targetType: LikeTargetType,

    /** UUID of the content being liked */
    @Column(name = "target_id", nullable = false)
    val targetId: UUID
) : BaseEntity() {

    /**
     * Checks if this like is for an original book cover.
     * @return true if the target is an original cover
     */
    fun isForOriginalCover(): Boolean = targetType == LikeTargetType.ORIGINAL_COVER

    /**
     * Checks if this like is for a translated book cover.
     * @return true if the target is a translated cover
     */
    fun isForTranslatedCover(): Boolean = targetType == LikeTargetType.TRANSLATED_COVER

    /**
     * Checks if this like is for a redesign.
     * @return true if the target is a redesign
     */
    fun isForRedesign(): Boolean = targetType == LikeTargetType.REDESIGN
}
