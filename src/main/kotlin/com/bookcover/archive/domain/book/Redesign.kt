package com.bookcover.archive.domain.book

import com.bookcover.archive.domain.common.BaseEntity
import com.bookcover.archive.domain.common.ContentStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

/**
 * Represents a redesigned book cover created by a user.
 * A redesign is a user's reinterpretation of an existing book cover.
 * Can be linked to either an OriginalBook or a TranslatedBook.
 *
 * Requirements:
 * - 3.1: Redesign must be linked to an original book or translated book
 * - 3.7: Redesign must be linked to the user who uploaded it
 */
@Entity
@Table(name = "redesigns")
class Redesign(
    /** URL to the full-resolution redesign image */
    @Column(name = "image_url", nullable = false)
    var imageUrl: String,

    /** URL to the thumbnail version of the redesign image */
    @Column(name = "thumbnail_url", nullable = false)
    var thumbnailUrl: String,

    /** Description of the redesign (design concept, inspiration, etc.) */
    @Column(name = "description", nullable = false, length = 2000)
    var description: String,

    /** The original book this redesign is based on (optional if translatedBook is set) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_book_id")
    var originalBook: OriginalBook? = null,

    /** The translated book this redesign is based on (optional if originalBook is set) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "translated_book_id")
    var translatedBook: TranslatedBook? = null,

    /** UUID of the user who created this redesign - Requirement 3.7 */
    @Column(name = "created_by_id", nullable = false)
    val createdById: UUID,

    /** Current publication status of the redesign */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: ContentStatus = ContentStatus.PUBLISHED,

    /** Number of likes this redesign has received */
    @Column(name = "like_count", nullable = false)
    var likeCount: Int = 0,

    /** Number of comments on this redesign */
    @Column(name = "comment_count", nullable = false)
    var commentCount: Int = 0
) : BaseEntity() {

    init {
        require(originalBook != null || translatedBook != null) {
            "Redesign must be linked to either an original book or a translated book"
        }
    }

    /**
     * Checks if this redesign is linked to an original book.
     * @return true if linked to an original book
     */
    fun isLinkedToOriginalBook(): Boolean = originalBook != null

    /**
     * Checks if this redesign is linked to a translated book.
     * @return true if linked to a translated book
     */
    fun isLinkedToTranslatedBook(): Boolean = translatedBook != null

    /**
     * Gets the book title this redesign is based on.
     * @return The title of the linked book
     */
    fun getBookTitle(): String = originalBook?.title ?: translatedBook?.title ?: ""

    /**
     * Increments the like count by 1.
     */
    fun incrementLikeCount() {
        likeCount++
    }

    /**
     * Decrements the like count by 1, ensuring it doesn't go below 0.
     */
    fun decrementLikeCount() {
        if (likeCount > 0) {
            likeCount--
        }
    }

    /**
     * Increments the comment count by 1.
     */
    fun incrementCommentCount() {
        commentCount++
    }

    /**
     * Decrements the comment count by 1, ensuring it doesn't go below 0.
     */
    fun decrementCommentCount() {
        if (commentCount > 0) {
            commentCount--
        }
    }
}
