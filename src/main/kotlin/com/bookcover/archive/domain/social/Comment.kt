package com.bookcover.archive.domain.social

import com.bookcover.archive.domain.common.BaseEntity
import com.bookcover.archive.domain.common.ContentStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.util.UUID

/**
 * Represents a comment on content (cover or redesign) by a user.
 * Uses polymorphic design to support comments on different content types.
 *
 * Requirements:
 * - 5.1: Store comment content, author, and creation time
 * - 5.3: Independent comment functionality for original covers, translated covers, and redesigns
 */
@Entity
@Table(name = "comments")
class Comment(
    /** The comment text content */
    @Column(name = "content", nullable = false, length = 1000)
    var content: String,

    /** UUID of the user who wrote this comment */
    @Column(name = "author_id", nullable = false)
    val authorId: UUID,

    /** Type of content being commented on */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    val targetType: CommentTargetType,

    /** UUID of the content being commented on */
    @Column(name = "target_id", nullable = false)
    val targetId: UUID,

    /** Current status of the comment */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: ContentStatus = ContentStatus.PUBLISHED
) : BaseEntity() {

    /**
     * Checks if this comment is for an original book cover.
     * @return true if the target is an original cover
     */
    fun isForOriginalCover(): Boolean = targetType == CommentTargetType.ORIGINAL_COVER

    /**
     * Checks if this comment is for a translated book cover.
     * @return true if the target is a translated cover
     */
    fun isForTranslatedCover(): Boolean = targetType == CommentTargetType.TRANSLATED_COVER

    /**
     * Checks if this comment is for a redesign.
     * @return true if the target is a redesign
     */
    fun isForRedesign(): Boolean = targetType == CommentTargetType.REDESIGN

    /**
     * Checks if the comment is visible (published).
     * @return true if the comment is published
     */
    fun isVisible(): Boolean = status == ContentStatus.PUBLISHED

    /**
     * Hides the comment (sets status to HIDDEN).
     */
    fun hide() {
        status = ContentStatus.HIDDEN
    }

    /**
     * Restores a hidden comment (sets status to PUBLISHED).
     */
    fun restore() {
        status = ContentStatus.PUBLISHED
    }

    /**
     * Marks the comment as deleted.
     */
    fun markDeleted() {
        status = ContentStatus.DELETED
    }

    /**
     * Updates the comment content.
     * @param newContent The new content for the comment
     * @throws IllegalArgumentException if the content is blank
     */
    fun updateContent(newContent: String) {
        require(newContent.isNotBlank()) { "Comment content cannot be blank" }
        content = newContent
    }
}
